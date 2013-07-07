package org.foxteam.noisyfox.tianyidialassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(SMS_RECEIVED)) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] msg = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					msg[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}

				PhoneNumberVerification nv = new PhoneNumberVerification(
						context);
				String phoneNumber = nv.getUnconfrimedNumber();

				for (SmsMessage curMsg : msg) {
					if (curMsg.getDisplayOriginatingAddress().equals("10001")) {
						String psw = Util.check(curMsg.getDisplayMessageBody());
						if (!psw.equals("")) {
							abortBroadcast();
							Util.recordLastPsw(context, psw);
							Util.showPswDialog(context, psw);
							// final Dialog d=new Dialog(arg0);
							// d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
							// d.show();

							// Toast.makeText(arg0, "Got The Password:" + psw,
							// Toast.LENGTH_LONG).show();

							// break;
						}
					} else if (curMsg.getDisplayOriginatingAddress().equals(
							phoneNumber)) {
						if (nv.confirmTextMessage(phoneNumber, curMsg
								.getDisplayMessageBody())) {
							abortBroadcast();
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
