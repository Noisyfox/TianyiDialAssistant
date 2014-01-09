package org.foxteam.noisyfox.tianyidialassistant;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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

	static final String STR_SERVER_URL = "http://192.168.0.13/tyserver/tyapp.php";
	static final String STR_SERVER_ARGS_KEY_METHOD = "method";
	static final String STR_SERVER_ARGS_KEY_NUMBER = "number";
	static final String STR_SERVER_ARGS_KEY_CODE = "code";
	static final String STR_SERVER_ARGS_KEY_PWD = "pwd";
	static final String STR_SERVER_ARGS_KEY_DEVICE_KEY = "device_key";
	static final String STR_SERVER_ARGS_KEY_CLIENT = "client";
	static final String STR_SERVER_ARGS_KEY_STAT = "stat";
	static final String STR_SERVER_ARGS_VALUE_GET = "get";
	static final String STR_SERVER_ARGS_VALUE_PUT = "put";
	static final String STR_SERVER_ARGS_VALUE_CLIENT = "TianyiDA/%s";
	static final String STR_SMS_NUMBER = "10001";
	static final String STR_SMS_CONTENT = "xykdmm";

	private final static UpdateThread updateThread = new UpdateThread();

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

		if (isUpdateAvailable()) {
			doUpdate();
		}
	}

	public void recordLastPsw(String psw) {
		synchronized (syncObj) {
			long time = System.currentTimeMillis();

			Editor e = mPreferences.edit();
			e.putString(SP_VALUE_STR_PSW, psw);
			e.putLong(SP_VALUE_LONG_TIME_GET, time);
			// e.putLong(SP_VALUE_LONG_TIME_REQUEST, time);

			e.commit();
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
				e.commit();
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
			} else if (dTimeUpdate > frequency && isUpdateAvailable()) {
				doUpdate(); // 提交服务器
			} else {
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
	private boolean isUpdateAvailable() {
		return mDefaultPreferences.getBoolean("enable_pc_assistant", false);
	}

	private void doUpdate() {
		if (!updateThread.isAlive())
			updateThread.start();
		updateThread.requireUpdate(this);
	}

	// 将储存的信息提交给服务器
	boolean update(String number, String code, String pwd, String device_key) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(STR_SERVER_ARGS_KEY_METHOD, STR_SERVER_ARGS_VALUE_PUT);
		map.put(STR_SERVER_ARGS_KEY_NUMBER, number);
		map.put(STR_SERVER_ARGS_KEY_CODE, code);
		map.put(STR_SERVER_ARGS_KEY_PWD, Util.hashString(pwd, "SHA"));
		map.put(STR_SERVER_ARGS_KEY_DEVICE_KEY, device_key);
		map.put(STR_SERVER_ARGS_KEY_STAT, mDefaultPreferences.getBoolean(
				"count_using_time", true) ? "1" : "0");
		String version = Util.getVersionName(mContext);
		if (version == null) {
			version = "unknown";
		}
		version = String.format(STR_SERVER_ARGS_VALUE_CLIENT, version);
		map.put(STR_SERVER_ARGS_KEY_CLIENT, version);

		String httpResult = Util.doHttpRequest(STR_SERVER_URL, map);
		// Toast.makeText(mContext, httpResult, Toast.LENGTH_LONG).show();
		if (httpResult == null) {
			new Exception("Failed to connect to server.").printStackTrace();
			return false;
		}

		// Log.d("", httpResult);

		try {
			JSONTokener jsonParser = new JSONTokener(httpResult);
			jsonParser.nextTo('{');
			if (!jsonParser.more()) {
				new Exception("Failed to read return value.").printStackTrace();
				return false;
			}
			JSONObject jsonObj = (JSONObject) jsonParser.nextValue();
			int result = jsonObj.getInt("result");
			if (result == 1) {
				return true;
			} else {
				// 记录日志
				int err_code = jsonObj.getInt("error_code");
				String err_msg = jsonObj.getString("error_msg");
				StringBuilder sb = new StringBuilder();
				sb.append("Update password failed!\nError code:");
				sb.append(String.format("%03X", err_code));
				sb.append("\nError message:");
				sb.append(err_msg);
				new Exception(sb.toString()).printStackTrace();
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
}
