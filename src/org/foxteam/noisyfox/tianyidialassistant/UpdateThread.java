package org.foxteam.noisyfox.tianyidialassistant;

import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class UpdateThread extends HandlerThread {

	Handler mHandler = null;
	UpdateRunnable updateRunable = null;

	public UpdateThread() {
		super("updateTimer");
		this.setPriority(Thread.MIN_PRIORITY);
		// this.setDaemon(true);
		this.start();
		mHandler = new Handler(this.getLooper());
	}

	public void requireUpdate(PSWOperator instance) {
		Log.d("", "Update required.");
		if (updateRunable != null) {
			mHandler.removeCallbacks(updateRunable);
		}
		updateRunable = new UpdateRunnable(instance);
		mHandler.postDelayed(updateRunable, 5000);
	}

	private class UpdateRunnable implements Runnable {
		final PSWOperator pswOperator;

		UpdateRunnable(PSWOperator instance) {
			pswOperator = instance;
		}

		@Override
		public void run() {
			new OperateThread(pswOperator).start();
		}
	}

	private class OperateThread extends Thread {

		PSWOperator pswOperator = null;

		OperateThread(PSWOperator instance) {
			super();
			pswOperator = instance;
		}

		@Override
		public void run() {
			Log.d("", "Update start.");

			PhoneNumberVerification pnv = new PhoneNumberVerification(
					pswOperator.mContext);
			if (pnv.isPhoneNumberConfirmed()) {
				String number = pnv.getPhoneNumber();
				String code = pswOperator.getLastPsw(false);
				long time = pswOperator.getRecordTime();
				EncryptedUploader uploader = new EncryptedUploader(pswOperator.mContext);
				
				if(uploader.isPaired()){
					boolean result = uploader.upload(number, code, time);
					
					if (result) {
						long currentTime = System.currentTimeMillis();
						Editor e = pswOperator.mPreferences.edit();
						e.putLong(PSWOperator.SP_VALUE_LONG_TIME_UPDATE,
								currentTime);
						e.commit();
					}
				}
			}
			Log.d("", "Update finish.");
		}

	}
}
