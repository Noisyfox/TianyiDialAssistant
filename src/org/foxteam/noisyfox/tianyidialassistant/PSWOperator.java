package org.foxteam.noisyfox.tianyidialassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PSWOperator {
    static final String SP_NAME = "PSWRecord";
    static final String SP_VALUE_STR_PSW = "psw";
    static final String SP_VALUE_LONG_TIME_GET = "time_get";
    static final String SP_VALUE_LONG_TIME_REQUEST = "time_request";
    static final String SP_VALUE_LONG_TIME_UPDATE = "time_update";

//    static final String STR_SERVER_URL = "http://192.168.0.13/tyserver/tyapp.php";
//    static final String STR_SERVER_ARGS_KEY_METHOD = "method";
//    static final String STR_SERVER_ARGS_KEY_NUMBER = "number";
//    static final String STR_SERVER_ARGS_KEY_CODE = "code";
//    static final String STR_SERVER_ARGS_KEY_PWD = "pwd";
//    static final String STR_SERVER_ARGS_KEY_DEVICE_KEY = "device_key";
//    static final String STR_SERVER_ARGS_KEY_CLIENT = "client";
//    static final String STR_SERVER_ARGS_KEY_STAT = "stat";
//    static final String STR_SERVER_ARGS_VALUE_GET = "get";
//    static final String STR_SERVER_ARGS_VALUE_PUT = "put";
//    static final String STR_SERVER_ARGS_VALUE_CLIENT = "TianyiDA/%s";
    static final String STR_SMS_NUMBER = "10001";
    static final String STR_SMS_CONTENT = "xykdmm";

//    private final static UpdateThread updateThread = new UpdateThread();

    Context mContext = null;
    SharedPreferences mPreferences = null;
    SharedPreferences mDefaultPreferences = null;

    static final Object syncObj = new Object();

    public PSWOperator(Context context) {
        mContext = context;
        mPreferences = mContext.getApplicationContext().getSharedPreferences(
                SP_NAME, Context.MODE_PRIVATE);
        mDefaultPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
    }

    public void requestNewPassword() {
        Util.sendSMS(mContext, STR_SMS_NUMBER, STR_SMS_CONTENT);
    }

    public void onSmsReceived(String psw) {
        recordLastPsw(psw);

        MyApplication.getPlanManager().postTask(
                MyApplication.getPlanManager().getAllPlans(0));

//        if (isUpdateAvailable()) {
//            doUpdate();
//        }
    }

    public void recordLastPsw(String psw) {
        synchronized (syncObj) {
            long time = System.currentTimeMillis();

            Editor e = mPreferences.edit();
            e.putString(SP_VALUE_STR_PSW, psw);
            e.putLong(SP_VALUE_LONG_TIME_GET, time);
            // e.putLong(SP_VALUE_LONG_TIME_REQUEST, time);

            e.apply();
        }
    }

    public String getLastPsw(boolean checkUpdate) {
        synchronized (syncObj) {
            long time = System.currentTimeMillis();
            String psw = mPreferences.getString(SP_VALUE_STR_PSW, "");
            if (checkUpdate) {
                if (!psw.equals("")) {
                    Long dTime_get = time
                            - mPreferences.getLong(SP_VALUE_LONG_TIME_GET, 0);
                    Long dTime_request = time
                            - mPreferences.getLong(SP_VALUE_LONG_TIME_REQUEST,
                            0);
                    if (dTime_get > 5.5 * 60 * 60 * 1000) {// 密码已经失效
                        psw = "";
                    } else if (dTime_request < 2 * 60 * 1000
                            && dTime_request > 20 * 1000
                            && dTime_request < dTime_get) {// 请求频繁，可能是密码失效了
                        psw = "";
                    }
                }
                Editor e = mPreferences.edit();
                e.putLong(SP_VALUE_LONG_TIME_REQUEST, time);
                e.apply();
            }
            return psw;
        }
    }

    public long getRecordTime() {
        return mPreferences.getLong(SP_VALUE_LONG_TIME_GET, 0);
    }

    // ==============================================================================================
    public boolean checkAndRefresh(boolean forceUpdate) {

        if (!forceUpdate) {
            long currentTime = System.currentTimeMillis();
            long frequency = Long.valueOf(mDefaultPreferences.getString(
                    "refresh_frequency", "300")) * 60 * 1000;
            // 检查上一次刷新时间
            long lstUpdateTime = mPreferences.getLong(
                    SP_VALUE_LONG_TIME_UPDATE, 0);
            long dTimeUpdate = currentTime - lstUpdateTime;
            long dTimeRecord = currentTime - getRecordTime();

            if (dTimeRecord > frequency) {
                requestNewPassword();
            } /*else if (dTimeUpdate > frequency && isUpdateAvailable()) {
                doUpdate(); // 提交服务器
            } */else {
                return false;
            }
        } else {
            requestNewPassword();
        }

        return true;
    }

    /**
     * 检测是否可以更新，具体指 有没有开启更新以及有没有进行配对
     *
     * @return
     */
//    private boolean isUpdateAvailable() {
//        return mDefaultPreferences.getBoolean("enable_pc_assistant", false);
//    }
//
//    private void doUpdate() {
//        if (!updateThread.isAlive())
//            updateThread.start();
//        updateThread.requireUpdate(this);
//    }

    public void storeLoginParam(String param){
        Editor e = mDefaultPreferences.edit();
        e.putString("lp", param);
        e.apply();
    }

    public String getLoginParam(){
        return mDefaultPreferences.getString("lp", null);
    }
}
