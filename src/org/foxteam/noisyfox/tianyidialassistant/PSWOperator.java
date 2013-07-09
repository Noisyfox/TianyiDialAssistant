package org.foxteam.noisyfox.tianyidialassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PSWOperator {
	static final String SP_NAME = "PSWRecord";
	static final String SP_VALUE_STR_PSW = "psw";
	static final String SP_VALUE_LONG_TIME_GET = "time_get";
	static final String SP_VALUE_LONG_TIME_REQUEST = "time_request";

	Context mContext = null;
	SharedPreferences mPreferences = null;

	static final Object syncObj = new Object();

	public PSWOperator(Context context) {
		mContext = context;
		mPreferences = mContext.getApplicationContext().getSharedPreferences(
				SP_NAME, Context.MODE_PRIVATE);
	}

	public void recordLastPsw(String psw) {
		synchronized (syncObj) {
			long time = System.currentTimeMillis();

			Editor e = mPreferences.edit();
			e.putString(SP_VALUE_STR_PSW, psw);
			e.putLong(SP_VALUE_LONG_TIME_GET, time);
			e.putLong(SP_VALUE_LONG_TIME_REQUEST, time);

			e.commit();
		}
	}

	public String getLastPsw(boolean update) {
		synchronized (syncObj) {
			long time = System.currentTimeMillis();
			String psw = mPreferences.getString(SP_VALUE_STR_PSW, "");
			if (update) {
				if (!psw.equals("")) {
					Long dTime_get = time
							- mPreferences.getLong(SP_VALUE_LONG_TIME_GET, 0);
					Long dTime_request = time
							- mPreferences.getLong(SP_VALUE_LONG_TIME_REQUEST,
									0);
					if (dTime_get > 3 * 60 * 60 * 1000) {// 密码已经失效
						psw = "";
					} else if (dTime_request < 2 * 60 * 1000
							&& dTime_request > 20 * 1000) {// 请求频繁，可能是密码失效了
						psw = "";
					}
				}
				Editor e = mPreferences.edit();
				e.putLong(SP_VALUE_LONG_TIME_REQUEST, time);
				e.commit();
			}
			return psw;
		}
	}
	
	public long getRecordTime(){
		return mPreferences.getLong(SP_VALUE_LONG_TIME_GET, 0);
	}
}
