package org.foxteam.noisyfox.tianyidialassistant;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class PhoneNumberVerification {
    static final String SP_NAME = "PhoneState";
    static final String SP_VALUE_STR_SAVEDNUMBER = "PhoneNumber";
    static final String SP_VALUE_STR_SAVEDNUMBER_ENCRYPT = "PhoneNumberEnc";
    static final String SP_VALUE_STR_UNCONFIRMEDNUMBER = "UPhoneNumber";
    static final String SP_VALUE_STR_CONFIRMSTRING = "ConfirmString";
    static final String SP_VALUE_BOOL_RUNATONCE = "RunAtOnce";

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

    // 是否提示过了
    boolean isRunAtOnce() {
        boolean rao = mPreferences.getBoolean(SP_VALUE_BOOL_RUNATONCE, false);
        return rao;
    }

    boolean isPhoneNumberConfirmed() {
        synchronized (syncObject) {
            // 获取储存的电话号码
            String savedPhoneNumber = mPreferences.getString(
                    SP_VALUE_STR_SAVEDNUMBER, "");
            if (savedPhoneNumber.equals("")) {
                return false;
            }
            String savedPhoneNumberEnc = mPreferences.getString(
                    SP_VALUE_STR_SAVEDNUMBER_ENCRYPT, "");
            if (savedPhoneNumberEnc.equals("")) {
                return false;
            }

            // 加密
            String enc = EncryptPhoneNumber(savedPhoneNumber);
            if (savedPhoneNumberEnc.equals(enc)) {
                return true;
            }
            return false;
        }
    }

    void beginConfirm() {
        synchronized (syncObject) {
            clearPhoneNumber();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    mContext);

            final AlertDialog alertDialog_ver = dialogBuilder.create();
            final AlertDialog alertDialog_edit = dialogBuilder.create();
            final AlertDialog alertDialog_skip = dialogBuilder.create();

            // 号码验证提示对话框
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

            // 号码输入对话框
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
                            if (number.length() == 0) {
                                Toast.makeText(mContext,
                                        R.string.dlgPhoneVer_failed,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                sendTextMessage(number);
                            }
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

            // 跳过验证对话框
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
    }

    void sendTextMessage(String number) {
        Toast.makeText(mContext, R.string.dlgPhoneVer_vering_toast,
                Toast.LENGTH_LONG).show();

        // 生成随机串
        Random randGen = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 5; i++)
            sb.append(String.format("%d", randGen.nextInt()));
        sb.append(number);
        String rk = EncryptPhoneNumber(sb.toString());

        Editor e = mPreferences.edit();
        e.clear();
        e.putString(SP_VALUE_STR_UNCONFIRMEDNUMBER, number);
        e.putString(SP_VALUE_STR_CONFIRMSTRING, rk);
        e.putBoolean(SP_VALUE_BOOL_RUNATONCE, true);
        e.commit();

        // 发送短信验证
        Util.sendSMS(mContext, number, rk);
    }

    boolean confirmTextMessage(String number, String msg) {
        synchronized (syncObject) {
            if (number == null || msg == null)
                return false;

            String confirmStr = mPreferences.getString(
                    SP_VALUE_STR_CONFIRMSTRING, "");
            if (confirmStr.equals("") || !number.equals(getUnconfrimedNumber())
                    || !msg.equals(confirmStr)) {
                return false;
            }
            String enc = EncryptPhoneNumber(number);
            if (enc == null)
                return false;

            Editor e = mPreferences.edit();
            e.clear();
            e.putString(SP_VALUE_STR_SAVEDNUMBER, number);
            e.putString(SP_VALUE_STR_SAVEDNUMBER_ENCRYPT, enc);
            e.putBoolean(SP_VALUE_BOOL_RUNATONCE, true);
            e.commit();

            return true;
        }
    }

    String getPhoneNumber() {
        synchronized (syncObject) {
            String savedPhoneNumber = mPreferences.getString(
                    SP_VALUE_STR_SAVEDNUMBER, "");
            if (savedPhoneNumber.equals(""))
                return null;

            return savedPhoneNumber;
        }
    }

    String getUnconfrimedNumber() {
        synchronized (syncObject) {
            String savedPhoneNumber = mPreferences.getString(
                    SP_VALUE_STR_UNCONFIRMEDNUMBER, "");
            if (savedPhoneNumber.equals(""))
                return null;

            return savedPhoneNumber;
        }
    }

    void clearPhoneNumber() {
        synchronized (syncObject) {
            Editor e = mPreferences.edit();
            e.clear();
            e.putBoolean(SP_VALUE_BOOL_RUNATONCE, true);
            e.commit();
        }
    }

    String getDeviceKey() {
        return mTelephonyManager.getSubscriberId();
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
