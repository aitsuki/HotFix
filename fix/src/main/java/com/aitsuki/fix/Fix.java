package com.aitsuki.fix;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import dalvik.system.DexClassLoader;

/**
 * Created AItsuki hp on 2016/4/11.
 */
public class Fix {

    private static Context mContext;
    private static SignatureChecker signature;

    public static void init(Context context) {
        signature = new SignatureChecker(context);
        mContext = context;
        File hackDir = context.getDir("hackDir", 0);
        File hackJar = new File(hackDir, "hack.jar");
        try {
            AssetsUtil.copyAssets(context, "hack.jar", hackJar.getAbsolutePath());
            load(hackJar.getAbsolutePath(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(String path) {
        load(path, true);
    }

    public static void load(String path, boolean doVerify) {
        File file = new File(path);
        if (!file.exists()) {
            Log.e("Fix", file.getName() + " does not exists");
            return;
        }

        if(doVerify && !signature.verifyApk(file))
            return;

        try {
            Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
            Object pathList = ReflectUtil.getField(cl, "pathList", mContext.getClassLoader());
            Object baseElements = ReflectUtil.getField(pathList.getClass(), "dexElements", pathList);
            String dexopt = mContext.getDir("dexopt", 0).getAbsolutePath();
            DexClassLoader dexClassLoader = new DexClassLoader(path, dexopt, dexopt, mContext.getClassLoader());
            Object obj = ReflectUtil.getField(cl, "pathList", dexClassLoader);
            Object dexElements = ReflectUtil.getField(obj.getClass(), "dexElements", obj);
            Object combineElements = ReflectUtil.combineArray(dexElements, baseElements);
            ReflectUtil.setField(pathList.getClass(), "dexElements", pathList, combineElements);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
