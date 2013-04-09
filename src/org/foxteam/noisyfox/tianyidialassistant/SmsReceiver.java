package org.foxteam.noisyfox.tianyidialassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		if (arg1.getAction().equals(SMS_RECEIVED)) {
			Bundle bundle = arg1.getExtras();
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] msg = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					msg[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}

				for (SmsMessage curMsg : msg) {
					if (curMsg.getDisplayOriginatingAddress().equals("10001")) {
						String psw = Util.check(curMsg.getDisplayMessageBody());
						if (!psw.equals("")) {
							abortBroadcast();
							Util.recordLastPsw(arg0, psw);
							Util.showPswDialog(arg0, psw);
							// final Dialog d=new Dialog(arg0);
							// d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
							// d.show();

							// Toast.makeText(arg0, "Got The Password:" + psw,
							// Toast.LENGTH_LONG).show();

							break;
						}
					}
				}
			}
			// Intent testIntent = new Intent(arg0, MainActivity.class);
			// testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// arg0.startActivity(testIntent);
		}
	}

}
