package org.foxteam.noisyfox.tianyidialassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class WidgetReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PSWOperator pswOper = new PSWOperator(context);
		String psw = pswOper.getLastPsw(true);


		if (psw.equals("")) {
			long currentTime = System.currentTimeMillis();

			SharedPreferences mPreferences = context.getApplicationContext()
					.getSharedPreferences("Runtime", Context.MODE_PRIVATE);
			Editor e = mPreferences.edit();
			e.putLong("WidgetRequired", currentTime);
			e.commit();
			
			pswOper.requestNewPassword();
		} else {
			Util.showPswDialog(context, psw);
		}
	}

}
