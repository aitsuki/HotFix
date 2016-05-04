package com.aitsuki.buildsrc

import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Created AItsuki hp on 2016/4/26.
 */
class FixPlugin implements Plugin<Project> {

    boolean debugOn
    boolean generatePatch
    String patchDir
    String patchName
    boolean minify

    File storeFile
    String storePassword
    String keyAlias
    String keyPassword

    @Override
    public void apply(Project project) {

        project.extensions.create("fixMode", FixExtension)
        project.extensions.create("fixSignConfig", SignExtension)

        project.afterEvaluate {
            FixUtils.init(project)

            // get Extension params
            def fixMode = project.extensions.findByName("fixMode") as FixExtension
            debugOn = fixMode.debugOn
            generatePatch = fixMode.generatePatch
            patchDir = fixMode.patchDir
            patchName = fixMode.patchName

            def signConfig =  project.extensions.findByName("fixSignConfig") as SignExtension
            storeFile = signConfig.storeFile
            storePassword = signConfig.storePassword
            keyAlias = signConfig.keyAlias
            keyPassword = signConfig.keyPassword

            def dexRelease = project.tasks.findByName("transformClassesWithDexForRelease")
            def dexDebug = project.tasks.findByName("transformClassesWithDexForDebug")
            def proguardRelease = project.tasks.findByName("transformClassesAndResourcesWithProguardForRelease")
            def proguardDebug = project.tasks.findByName("transformClassesAndResourcesWithProguardForDebug")

            if (proguardRelease) {
                proguardReleaseClosure(proguardRelease)
            }

            if (proguardDebug) {
                proguardDebugClosure(proguardDebug)
            }

            if (dexRelease) {
                dexReleaseClosure(dexRelease)
            }

            if (dexDebug) {
                if (!debugOn && !generatePatch) {
                    // nothing to do
                } else {
                    dexDebugClosure(dexDebug)
                }
            }
        }
    }

    def proguardReleaseClosure = { Task proguardRelease ->
        proguardRelease.doFirst {
            minify = true
        }

        // copy mapping.txt to app rootDir
        proguardRelease.doLast {
            File file = new File("$project.buildDir\\outputs\\mapping\\release\\mapping.txt")
            if (file.exists()) {
                FixUtils.copyFile(file, new File(project.projectDir, "mapping.txt"))
            }
        }
    }

    def proguardDebugClosure = { Task proguardDebug ->

        proguardDebug.doFirst {
            File mappingFile = new File(FixUtils.mappingPath)
            if (mappingFile.exists()) {
                def transformTask = (TransformTask) proguardDebug
                def transform = (ProGuardTransform) transformTask.getTransform()
                transform.applyTestedMapping(mappingFile)
            } else {
                String tips = "mapping.txt not found, you can run 'Generate Signed Apk' with release and minify to generate a mapping, or setting generatePath false"
                throw new IllegalStateException(tips)
            }
            minify = true
        }
    }

    def dexDebugClosure = { Task dexDebug ->

        dexDebug.outputs.upToDateWhen { false }

        dexDebug.doFirst {
            Map<String, String> md5Map
            if (generatePatch) {
                // clear previous patch
                File file = new File(patchDir)
                if (file.exists()) {
                    FixUtils.cleanDirectory(file)
                }

                // resolve hash.txt (entry-> className : md5)
                File hashFile = new File(FixUtils.hashPath)
                if (hashFile.exists()) {
                    md5Map = FixUtils.resolveHashFile(hashFile)
                } else {
                    String tips = "hash.txt not found, you must run 'Generate Signed Apk' at first or setting generatePath false"
                    throw new IllegalStateException(tips)
                }
            }

            if (minify) {
                dexDebug.inputs.files.files.each { File file ->
                    file.eachFileRecurse FileType.FILES, { File f ->
                        if (f.absolutePath.endsWith('.jar')) {
                            FixUtils.processJar(f, debugOn, generatePatch, md5Map, patchDir, true)
                        }
                    }
                }

            } else {
                dexDebug.inputs.files.files.each { File file ->
                    if (file.name.endsWith('.jar') && FixUtils.shouldProcessJar(file.absolutePath)) {
                        FixUtils.processJar(file, debugOn, generatePatch, md5Map, patchDir, false)
                    } else if (file.isDirectory()) {
                        FixUtils.processDir(file, debugOn, generatePatch, md5Map, patchDir, false)
                    }
                }
            }

            if (generatePatch) {
                FixUtils.dx(project, patchDir,patchName)
                FixUtils.signApk(new File(patchDir, patchName),storeFile,keyPassword,storePassword,keyAlias)
            }
        }
    }

    def dexReleaseClosure = { Task dexRelease ->

        // not up-to-date
        // http://stackoverflow.com/questions/7289874/resetting-the-up-to-date-property-of-gradle-tasks
        dexRelease.outputs.upToDateWhen { false }

        // generate hash.txt and inject code in .class
        dexRelease.doFirst {
            File hashFile = FixUtils.createHashFile()
            def writer = hashFile.newPrintWriter()
            // if minify, outputs always is endsWith .jar in "build/intermediates/transforms/proguard/……"
            // else, inputs directory path is "build/intermediates/classes/……"
            if (minify) {
                dexRelease.inputs.files.files.each { File file ->
                    file.eachFileRecurse FileType.FILES, { File f ->
                        if (f.absolutePath.endsWith('.jar')) {
                            FixUtils.processJar(f, writer, true)
                        }
                    }
                }
            } else {
                dexRelease.inputs.files.files.each { File file ->
                    if (file.name.endsWith('.jar') && FixUtils.shouldProcessJar(file.absolutePath)) {
                        FixUtils.processJar(file, writer, false)
                    } else if (file.isDirectory()) {
                        FixUtils.processDir(file, writer)
                    }
                }
            }
            writer.close()
        }
    }

}
