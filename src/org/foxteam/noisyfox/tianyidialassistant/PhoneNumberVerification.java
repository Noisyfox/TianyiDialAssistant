package org.foxteam.noisyfox.tianyidialassistant;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PhoneNumberVerification {
	static final String SP_NAME = "PhoneState";
	static final String SP_VALUE_SAVEDNUMBER = "PhoneNumber";
	static final String SP_VALUE_SAVEDNUMBER_ENCRYPT = "PhoneNumberEnc";
	static final String SP_VALUE_UNCONFIRMEDNUMBER = "UPhoneNumber";
	static final String SP_VALUE_CONFIRMSTRING = "ConfirmString";

	private final static Object syncObject = new Object();
	private Context mContext = null;
	private TelephonyManager mTelephonyManager = null;
	private SharedPreferences mPreferences = null;

	PhoneNumberVerification(Context context) {
		mContext = context;
		mTelephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		mPreferences = context.getApplicationContext().getSharedPreferences(
				SP_NAME, Context.MODE_PRIVATE);
	}

	boolean isPhoneNumberConfirmed() {
		synchronized (syncObject) {
			// ��ȡ����ĵ绰����
			String savedPhoneNumber = mPreferences.getString(
					SP_VALUE_SAVEDNUMBER, "");
			if (savedPhoneNumber.equals("")) {
				return false;
			}
			String savedPhoneNumberEnc = mPreferences.getString(
					SP_VALUE_SAVEDNUMBER_ENCRYPT, "");
			if (savedPhoneNumberEnc.equals("")) {
				return false;
			}

			// ����
			String enc = EncryptPhoneNumber(savedPhoneNumber);
			if (savedPhoneNumberEnc.equals(enc)) {
				return true;
			}
			return false;
		}
	}

	void beginConfirm() {
		// new Thread(){
		// @Override
		// public void run() {
		synchronized (syncObject) {
			clearPhoneNumber();

			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					mContext);

			final AlertDialog alertDialog_ver = dialogBuilder.create();
			final AlertDialog alertDialog_edit = dialogBuilder.create();
			final AlertDialog alertDialog_skip = dialogBuilder.create();

			// ������֤��ʾ�Ի���
			alertDialog_ver.setMessage(mContext
					.getText(R.string.dlgPhoneVer_noti_text));
			// ad.setView(v);
			alertDialog_ver.setCancelable(false);
			alertDialog_ver.setTitle(R.string.dlgPhoneVer_noti_title);
			alertDialog_ver.setButton(Dialog.BUTTON_POSITIVE,
					mContext.getText(R.string.button_ok),
					new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							alertDialog_edit.show();
						}
					});

			// ��������Ի���
			View v = View.inflate(mContext, R.layout.phone_edit_dialog, null);
			alertDialog_edit.setView(v, 5, 5, 5, 5);
			alertDialog_edit.setCancelable(false);
			alertDialog_edit.setTitle(R.string.dlgPhoneVer_edit_title);
			alertDialog_edit.setButton(Dialog.BUTTON_POSITIVE,
					mContext.getText(R.string.button_ok),
					new Dialog.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							EditText et = (EditText) alertDialog_edit
									.getWindow().findViewById(
											R.id.editText_dlgPhoneVer_edit);
							String number = et.getText().toString();

							sendTextMessage(number);
						}

					});
			alertDialog_edit.setButton(Dialog.BUTTON_NEGATIVE,
					mContext.getText(R.string.button_skip),
					new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							alertDialog_skip.show();
						}
					});

			// ������֤�Ի���
			alertDialog_skip.setCancelable(false);
			alertDialog_skip.setTitle(R.string.dlgPhoneVer_skip_title);
			alertDialog_skip.setMessage(mContext
					.getText(R.string.dlgPhoneVer_skip_text));
			alertDialog_skip.setButton(Dialog.BUTTON_POSITIVE,
					mContext.getText(R.string.button_ok),
					new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(mContext,
									R.string.dlgPhoneVer_skipped_toast,
									Toast.LENGTH_LONG).show();
						}
					});
			alertDialog_skip.setButton(Dialog.BUTTON_NEGATIVE,
					mContext.getText(R.string.button_cancel),
					new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							alertDialog_edit.show();
						}
					});

			alertDialog_ver.show();
		}
		// }
		// }.start();
	}

	void sendTextMessage(String number) {
		Toast.makeText(mContext, R.string.dlgPhoneVer_vering_toast,
				Toast.LENGTH_LONG).show();

		// ���������
		Random randGen = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 5; i++)
			sb.append(String.format("%d", randGen.nextInt()));
		sb.append(number);
		String rk = EncryptPhoneNumber(sb.toString());

		Editor e = mPreferences.edit();
		e.clear();
		e.putString(SP_VALUE_UNCONFIRMEDNUMBER, number);
		e.putString(SP_VALUE_CONFIRMSTRING, rk);
		e.commit();

		// ���Ͷ�����֤
		Util.sendSMS(mContext, number, rk);
	}

	boolean confirmTextMessage(String number, String msg) {
		synchronized (syncObject) {
			if (number == null || msg == null)
				return false;

			String confirmStr = mPreferences.getString(SP_VALUE_CONFIRMSTRING,
					"");
			if (confirmStr.equals("") || !number.equals(getUnconfrimedNumber())
					|| !msg.equals(confirmStr)) {
				return false;
			}
			String enc = EncryptPhoneNumber(number);
			if (enc == null)
				return false;

			Editor e = mPreferences.edit();
			e.clear();
			e.putString(SP_VALUE_SAVEDNUMBER, number);
			e.putString(SP_VALUE_SAVEDNUMBER_ENCRYPT, enc);
			e.commit();

			return true;
		}
	}

	String getPhoneNumber() {
		synchronized (syncObject) {
			String savedPhoneNumber = mPreferences.getString(
					SP_VALUE_SAVEDNUMBER, "");
			if (savedPhoneNumber.equals(""))
				return null;

			return savedPhoneNumber;
		}
	}

	String getUnconfrimedNumber() {
		synchronized (syncObject) {
			String savedPhoneNumber = mPreferences.getString(
					SP_VALUE_UNCONFIRMEDNUMBER, "");
			if (savedPhoneNumber.equals(""))
				return null;

			return savedPhoneNumber;
		}
	}

	void clearPhoneNumber() {
		synchronized (syncObject) {
			Editor e = mPreferences.edit();
			// e.remove(SP_VALUE_SAVEDNUMBER);
			// e.remove(SP_VALUE_SAVEDNUMBER_ENCRYPT);
			// e.remove(SP_VALUE_UNCONFIRMEDNUMBER);
			// e.remove(SP_VALUE_CONFIRMSTRING);
			e.clear();
			e.commit();
		}
	}

	private final String EncryptPhoneNumber(String number) {
		if (number == null)
			return null;

		String imsi = mTelephonyManager.getSubscriberId();
		if (imsi == null)
			return null;

		byte btInput[] = number.getBytes();
		byte md[] = null;
		MessageDigest mdInst = null;
		try {
			mdInst = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		mdInst.update(btInput);
		md = mdInst.digest();

		byte btImsi[] = imsi.getBytes();
		for (int i = 0; i < md.length; i++) {
			md[i] |= btImsi[i % btImsi.length];
		}

		mdInst.reset();
		mdInst.update(md);
		md = mdInst.digest();

		StringBuilder sb = new StringBuilder(64);
		for (byte b : md) {
			sb.append(String.format("%02X", b));
		}

		return sb.toString();
	}
}
