package org.foxteam.noisyfox.tianyidialassistant;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.emar.escore.banner.BannerSDK;
import com.emar.escore.sdk.YjfSDK;
import com.emar.escore.sdk.view.bannerView;
import com.emar.escore.sdk.widget.UpdateScordNotifier;

public class MainActivity extends Activity implements UpdateScordNotifier {
	public static final int MSG_PHONE_NUMBER_VERIFICATION_SUCCESS = 1;
	public static final int MSG_PHONE_NUMBER_VERIFICATION_START = 2;
	public static final int MSG_UPDATE_MAIN_TEXT = 3;

	static MainActivity mainActivity;
	TextView t;
	LinearLayout linearLayout;

	Handler mainHandler = new MyHander(this);

	static class MyHander extends Handler {
		WeakReference<MainActivity> mActivityRef;

		MyHander(MainActivity activity) {
			mActivityRef = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = mActivityRef.get();
			if (activity == null)
				return;
			switch (msg.what) {
			case MSG_PHONE_NUMBER_VERIFICATION_SUCCESS:
				break;
			case MSG_PHONE_NUMBER_VERIFICATION_START:
				PhoneNumberVerification pnv = new PhoneNumberVerification(
						activity);
				pnv.beginConfirm();
				break;
			case MSG_UPDATE_MAIN_TEXT:
				String w = Util.updateMainText(activity);
				if (w.equals("")) {
					activity.t.setText(R.string.psw_neverget);
				} else {
					activity.t.setText(w);
				}
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mainActivity = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		linearLayout = (LinearLayout) findViewById(R.id.banner_linear);

		boolean show_add = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean("checkbox_advertisement", true);

		// 当Activity第一次创建时调用,此方法需继承接口UpdateScordNotifier
		YjfSDK.getInstance(this, this).initInstance("", "", "", "");
		// Banner广告---------------------------------------------------------------------------------------------------------------------------------
		if (show_add) {
			// 显示推广条
			final bannerView bannerView = BannerSDK.getInstance(this)
					.getBanner();
			linearLayout.addView(bannerView);
			// 推广条轮转
			BannerSDK.getInstance(MainActivity.this).showBanner(bannerView);
		}
		// Banner广告---------------------------------------------------------------------------------------------------------------------------------

		Util.registerReceiver(this);
		t = (TextView) this.findViewById(R.id.textView_info);
		Button b = (Button) this.findViewById(R.id.button_getnow);

		String w = Util.updateMainText(this);
		if (w.equals("")) {
			t.setText(R.string.psw_neverget);
		} else {
			t.setText(w);
		}

		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Util.sendSMS(mainActivity, "10001", "xykdmm");
				t.setText("获取中，请耐心等待哦~");
			}

		});

		SmsReceiver.registerActivity(this);

		PhoneNumberVerification pnv = new PhoneNumberVerification(this);
		if (pnv.isPhoneNumberConfirmed()) {
			// Toast.makeText(this, pnv.getPhoneNumber(), Toast.LENGTH_LONG)
			// .show();
		} else if (!pnv.isRunAtOnce()) {
			mainHandler.sendMessage(mainHandler
					.obtainMessage(MSG_PHONE_NUMBER_VERIFICATION_START));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		String w = Util.updateMainText(this);
		if (w.equals("")) {
			t.setText(R.string.psw_neverget);
		} else {
			t.setText(w);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intent = new Intent();
			intent.setClass(this, SettingsActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		Util.unregisterReceiver(this);
		YjfSDK.getInstance(this, this).recordAppClose();
		super.onDestroy();
	}

	@Override
	public void updateScoreFailed(int arg0, int arg1, String arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateScoreSuccess(int arg0, int arg1, int arg2, String arg3) {
		// TODO Auto-generated method stub

	}

}
