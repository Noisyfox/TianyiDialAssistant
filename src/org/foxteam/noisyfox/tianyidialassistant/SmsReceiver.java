package org.foxteam.noisyfox.tianyidialassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsMessage;

import java.lang.ref.WeakReference;

public class SmsReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    static WeakReference<TyMainActivity> mActivityRef = null;

    public static void registerActivity(TyMainActivity tyMainActivity) {
        if (tyMainActivity != null) {
            mActivityRef = new WeakReference<TyMainActivity>(tyMainActivity);
        }
    }

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

//                PhoneNumberVerification nv = new PhoneNumberVerification(
//                        context);
//                String unconfirmedNumber = nv.getUnconfrimedNumber();

                Handler handler = null;
                if (mActivityRef != null) {
                    TyMainActivity act = mActivityRef.get();
                    if (act != null) {
                        handler = act.mainHandler;
                    }
                }

                for (SmsMessage curMsg : msg) {
                    String addr = curMsg.getDisplayOriginatingAddress();
                    if (addr.equals("10001") || addr.equals("16300")) {
                        String psw = Util.check(curMsg.getDisplayMessageBody());
                        if (!psw.equals("")) {
                            abortBroadcast();
                            PSWOperator pswOper = new PSWOperator(context);
                            pswOper.onSmsReceived(psw);

                            SharedPreferences mPreferences = context
                                    .getApplicationContext()
                                    .getSharedPreferences("Runtime",
                                            Context.MODE_PRIVATE);
                            long currentTime = System.currentTimeMillis();
                            long lstTime = mPreferences.getLong(
                                    "WidgetRequired", 0);
                            long dTime = currentTime - lstTime;
                            if (dTime < 3 * 60 * 1000) {
                                Util.showPswDialog(context, psw);
                            }

                            if (handler != null)
                                handler.sendMessage(handler
                                        .obtainMessage(TyMainActivity.MSG_UPDATE_MAIN_TEXT));
                            // final Dialog d=new Dialog(arg0);
                            // d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            // d.show();

                            // Toast.makeText(arg0, "Got The Password:" + psw,
                            // Toast.LENGTH_LONG).show();

                            // break;
                        }
                    }/* else if (addr.equals(unconfirmedNumber)) {
                        if (nv.confirmTextMessage(unconfirmedNumber,
                                curMsg.getDisplayMessageBody())) {
                            if (handler != null)
                                handler.sendMessage(handler
                                        .obtainMessage(TyMainActivity.MSG_PHONE_NUMBER_VERIFICATION_SUCCESS));
                            abortBroadcast();
                        }
                    }*/
                }
            }
            // Intent testIntent = new Intent(arg0, MainActivity.class);
            // testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // arg0.startActivity(testIntent);
        }
    }
}
