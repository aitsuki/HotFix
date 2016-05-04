package com.aitsuki.hotfix;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.aitsuki.fix.Fix;

/**
 * Created by AItsuki on 2016/4/26.
 *
 */
public class FixApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Fix.init(this);
        Fix.load(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+"patch.jar");
    }
}
