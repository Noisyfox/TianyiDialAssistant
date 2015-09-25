package org.foxteam.noisyfox.tianyidialassistant;

import android.app.Application;

public class MyApplication extends Application {

    private static PlanManager mPlanManager = null;
    private static OpenWrtHelper mOWHelper = new OpenWrtHelper();

    @Override
    public void onCreate() {
        super.onCreate();
        mPlanManager = new PlanManager(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        mPlanManager.shutdownTask();

        super.onTerminate();
    }

    public static PlanManager getPlanManager() {
        return mPlanManager;
    }

    public static OpenWrtHelper getOpenWrtHelper() {
        return mOWHelper;
    }
}
