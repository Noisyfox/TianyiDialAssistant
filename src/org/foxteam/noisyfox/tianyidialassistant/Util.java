package org.foxteam.noisyfox.tianyidialassistant;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Util {
	public static String check(String paramString) {
		Matcher localMatcher = Pattern.compile("(?<=密码)\\d{6}").matcher(
				paramString);
		if (localMatcher.find())
			return localMatcher.group();
		return "";
	}

	static String SENT_SMS_ACTION = "SENT_SMS_ACTION";
	static String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
	static BroadcastReceiver BR_SENT_SMS_ACTION = null;
	static BroadcastReceiver BR_DELIVERED_SMS_ACTION = null;

	/**
	 * Send SMS
	 * 
	 * @param context
	 * @param phoneNumber
	 * @param message
	 */
	public static void sendSMS(Context context, String phoneNumber,
			String message) {
		// create the sentIntent parameter
		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
				sentIntent, 0);
		// create the deilverIntent parameter
		Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
		PendingIntent deliverPI = PendingIntent.getBroadcast(context, 0,
				deliverIntent, 0);

		SmsManager sms = SmsManager.getDefault();
		if (message.length() > 70) {
			List<String> msgs = sms.divideMessage(message);
			for (String msg : msgs) {
				sms.sendTextMessage(phoneNumber, null, msg, sentPI, deliverPI);
			}
		} else {
			sms.sendTextMessage(phoneNumber, null, message, sentPI, deliverPI);
		}
		Toast.makeText(context, "短信发送中", Toast.LENGTH_SHORT).show();

	}

	/**
	 * register the Broadcast Receivers
	 * 
	 * @param context
	 */
	public static void registerReceiver(Context context) {
		if (BR_SENT_SMS_ACTION == null) {
			BR_SENT_SMS_ACTION = new BroadcastReceiver() {
				@Override
				public void onReceive(Context _context, Intent _intent) {
					switch (getResultCode()) {
					case Activity.RESULT_OK:
						Toast.makeText(_context, "短信发送成功", Toast.LENGTH_SHORT)
								.show();
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						Toast.makeText(_context,
								"短信发送失败:SMS generic failure actions",
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						Toast.makeText(_context,
								"短信发送失败:SMS radio off failure actions",
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						Toast.makeText(_context,
								"短信发送失败:SMS null PDU failure actions",
								Toast.LENGTH_SHORT).show();
						break;
					}
				}
			};
		}
		if (BR_DELIVERED_SMS_ACTION == null) {
			BR_DELIVERED_SMS_ACTION = new BroadcastReceiver() {
				@Override
				public void onReceive(Context _context, Intent _intent) {
					Toast.makeText(_context, "SMS delivered actions",
							Toast.LENGTH_SHORT).show();
				}
			};
		}
		context.registerReceiver(BR_SENT_SMS_ACTION, new IntentFilter(
				SENT_SMS_ACTION));
		context.registerReceiver(BR_DELIVERED_SMS_ACTION, new IntentFilter(
				DELIVERED_SMS_ACTION));
	}

	/**
	 * unregister the Broadcast Receivers
	 * 
	 * @param context
	 */
	public static void unregisterReceiver(Context context) {
		context.unregisterReceiver(BR_SENT_SMS_ACTION);
		context.unregisterReceiver(BR_DELIVERED_SMS_ACTION);
	}

	private static final Object pswDlgSyncObject = new Object();
	private static Dialog pswDialog = null;

	public static void showPswDialog(Context context, String psw) {
		synchronized (pswDlgSyncObject) {
			// final Dialog d = new Dialog(arg0);
			// d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			// d.show();
			if (pswDialog != null) {
				pswDialog.dismiss();
				pswDialog = null;
			}
			View v = View.inflate(context, R.layout.psw_dialog, null);

			pswDialog = new AlertDialog.Builder(context).setView(v).create();
			pswDialog.getWindow().setType(
					WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

			pswDialog.show();

			TextView t = (TextView) pswDialog.getWindow().findViewById(
					R.id.pswView);
			t.setText(psw);

			Button b = (Button) pswDialog.getWindow().findViewById(
					R.id.button_pswdialog_dismiss);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					synchronized (pswDlgSyncObject) {
						if (pswDialog != null) {
							pswDialog.dismiss();
							pswDialog = null;
						}
					}
				}
			});
		}
	}

	public static void safeClose(Closeable c) {
		try {
			if (c != null)
				c.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
