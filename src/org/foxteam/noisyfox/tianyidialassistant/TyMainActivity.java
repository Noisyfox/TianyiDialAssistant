package org.foxteam.noisyfox.tianyidialassistant;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.widget.TabHost;

public class TyMainActivity extends SherlockFragmentActivity {
	public static final int REQUEST_SETTINGS = 1;

	public static final int MSG_PHONE_NUMBER_VERIFICATION_SUCCESS = 1;
	public static final int MSG_PHONE_NUMBER_VERIFICATION_START = 2;
	public static final int MSG_UPDATE_MAIN_TEXT = 3;

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	PhoneNumberVerification mPhoneNumberVerification = null;
	PSWOperator mPSWOperator = null;

	Handler mainHandler = new MyHander(this);

	static class MyHander extends Handler {
		WeakReference<TyMainActivity> mActivityRef;

		MyHander(TyMainActivity activity) {
			mActivityRef = new WeakReference<TyMainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			TyMainActivity activity = mActivityRef.get();
			if (activity == null)
				return;
			switch (msg.what) {
			case MSG_PHONE_NUMBER_VERIFICATION_SUCCESS:
				break;
			case MSG_PHONE_NUMBER_VERIFICATION_START:
				activity.mPhoneNumberVerification.beginConfirm();
				break;
			case MSG_UPDATE_MAIN_TEXT:
				activity.mViewPager.getChildAt(0);
				TyAssistantFragment taf = (TyAssistantFragment) activity
						.findRegisteredFragment(TyAssistantFragment.TAG);
				if (taf != null) {
					taf.updateMainText();
				}
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock_Light);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ty_main);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();
		mViewPager = (ViewPager) findViewById(R.id.pager);

		mPhoneNumberVerification = new PhoneNumberVerification(this);
		mPSWOperator = new PSWOperator(this);
		Util.registerReceiver(this);
		SmsReceiver.registerActivity(this);
		AlarmReceiver.startAlarm(this);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
		mTabsAdapter.addTab(
				mTabHost.newTabSpec("tyassistant").setIndicator(
						getText(R.string.title_activity_tyassistant)),
				TyAssistantFragment.class, null);
		mTabsAdapter.addTab(
				mTabHost.newTabSpec("openwrt").setIndicator(
						getText(R.string.title_activity_openwrt)),
				OpenWrtFragment.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		if (mPhoneNumberVerification.isPhoneNumberConfirmed()) {
			// Toast.makeText(this, pnv.getPhoneNumber(), Toast.LENGTH_LONG)
			// .show();

			// mPSWOperator.update(mPhoneNumberVerification.getPhoneNumber(),
			// mPSWOperator.getLastPsw(false), "123456",
			// mPhoneNumberVerification.getDeviceKey());
		} else if (!mPhoneNumberVerification.isRunAtOnce()) {
			mainHandler.sendMessage(mainHandler
					.obtainMessage(MSG_PHONE_NUMBER_VERIFICATION_START));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.ty_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intent = new Intent();
			intent.setClass(this, SettingsActivity.class);
			this.startActivityForResult(intent, REQUEST_SETTINGS);
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SETTINGS) {
			switch (resultCode) {
			case SettingsActivity.RESULT_REQUIRE_PHONEVER:
				mainHandler.sendMessage(mainHandler
						.obtainMessage(MSG_PHONE_NUMBER_VERIFICATION_START));
				break;
			case SettingsActivity.RESULT_OK:
			default:
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
	}

	@Override
	protected void onDestroy() {
		Util.unregisterReceiver(this);
		super.onDestroy();
	}

	HashMap<String, Fragment> mRegisteredFragment = new HashMap<String, Fragment>();

	public void registerFragment(Fragment fragment, String tag) {
		mRegisteredFragment.put(tag, fragment);
	}

	public void unregisterFragment(String tag) {
		mRegisteredFragment.remove(tag);
	}

	public Fragment findRegisteredFragment(String tag) {
		return mRegisteredFragment.get(tag);
	}
}
