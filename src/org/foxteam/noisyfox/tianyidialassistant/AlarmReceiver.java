package org.foxteam.noisyfox.tianyidialassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class AlarmReceiver extends BroadcastReceiver {
	public static final String ALARM_ACTION_UPDATEPSW = "org.foxteam.noisyfox.tianyidialassistant.updatePassword";
	public static final String SP_VALUE_LONG_LAST_CHECK_TIME = "LastCheckTime";
	public static final int SENDER_CODE_UPDATEPSW = 0;
	public static final long INTERVAL_UPDATEPSW = 60 * 1000;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (ALARM_ACTION_UPDATEPSW.equals(intent.getAction())) {
			PhoneNumberVerification pnv = new PhoneNumberVerification(context);
			SharedPreferences defaultPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (pnv.isPhoneNumberConfirmed()
					&& defaultPreferences.getBoolean("enable_pc_assistant",
							false)) {

				long currentTime = System.currentTimeMillis();
				SharedPreferences sharedPreferences = context
						.getApplicationContext().getSharedPreferences(
								"Runtime", Context.MODE_PRIVATE);
				long lastCheckTime = sharedPreferences.getLong(
						SP_VALUE_LONG_LAST_CHECK_TIME, 0);

				if (currentTime - lastCheckTime > 3 * 60 * 1000) {
					PSWOperator pswOperator = new PSWOperator(context);
					boolean result = pswOperator.checkAndUpdate(false);
					if (result) {
						Editor e = sharedPreferences.edit();
						e.putLong(SP_VALUE_LONG_LAST_CHECK_TIME, currentTime);
						e.commit();
					}
				}
			}
			// return;
			// Util.showPswDialog(context, "tick!");
		}
	}

	public static void startAlarm(Context context) {
		Intent intent = new Intent(ALARM_ACTION_UPDATEPSW);
		PendingIntent pi = PendingIntent.getBroadcast(context,
				SENDER_CODE_UPDATEPSW, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
				INTERVAL_UPDATEPSW, pi);
	}
}
