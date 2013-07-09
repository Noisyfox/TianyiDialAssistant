package org.foxteam.noisyfox.tianyidialassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PSWOperator pswOper = new PSWOperator(context);
		String psw = pswOper.getLastPsw(true);

		if (psw.equals("")) {
			Util.sendSMS(context, "10001", "xykdmm");
		} else {
			Util.showPswDialog(context, psw);
		}
	}

}
