package com.aitsuki.buildsrc;

/**
 * Created by AItsuki on 2016/4/28.
 *
 */
public class FixExtension {
    /**
     * 在debug模式下，是否注入代码
     */
    boolean debugOn = true

    /**
     * 是否自动打包补丁
     */
    boolean generatePatch = true

    /**
     * 自动打包补丁后保存在哪个目录
     */
    String patchDir = "C:\\Users\\Default\\hotfix"

    /**
     * 补丁的名字
     */
    String patchName = "patch.jar"
}
