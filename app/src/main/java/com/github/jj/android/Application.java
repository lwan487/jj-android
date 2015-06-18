package com.github.jj.android;

import me.yugy.app.common.utils.DebugUtils;

public class Application extends android.app.Application {

    private static Application sInstance;

    public static Application getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        DebugUtils.setLogEnable(BuildConfig.DEBUG);
    }


}
