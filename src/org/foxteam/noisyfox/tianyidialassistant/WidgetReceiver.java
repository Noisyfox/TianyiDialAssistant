package org.foxteam.noisyfox.tianyidialassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		String psw = Util.getLastPsw(arg0);
		if (psw.equals("")) {
			Util.sendSMS(arg0, "10001", "xykdmm");
		} else {
			Util.showPswDialog(arg0, psw);
		}
	}

}
