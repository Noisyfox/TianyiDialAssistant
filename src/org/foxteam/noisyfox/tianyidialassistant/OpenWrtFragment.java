package org.foxteam.noisyfox.tianyidialassistant;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;

public class OpenWrtFragment extends SherlockFragment {
	static final String TAG = "OpenWrtFragment";

	TyMainActivity father = null;
	OpenWrtHelper opHelper = new OpenWrtHelper();

	ImageView iv = null;
	EditText editText_psw_override = null;
	CheckBox checkBox_psw_override = null;

	String progressText;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		father = (TyMainActivity) activity;
		father.registerFragment(this, TAG);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		progressText = getString(R.string.ssh_working);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		father.unregisterFragment(TAG);
		opHelper.shutdown();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_openwrt, container,
				false);

		iv = (ImageView) rootView.findViewById(R.id.imageView_status);
		editText_psw_override = (EditText) rootView
				.findViewById(R.id.editText_psw_override);
		checkBox_psw_override = (CheckBox) rootView
				.findViewById(R.id.checkBox_psw_override);

		updateStatus(OpenWrtHelper.STATUS_UNKNOWN);

		rootView.findViewById(R.id.button_check_status).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);
						new Thread() {
							@Override
							public void run() {
								int status = opHelper.checkDialStatus();

								father.mainHandler.sendMessage(father.mainHandler
										.obtainMessage(
												TyMainActivity.MSG_UPDATE_PPPOE_STATUS,
												status, 0));
								father.hideProgress();
							}

						}.start();
					}
				});

		rootView.findViewById(R.id.button_pppoe_connect).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);
						new Thread() {
							@Override
							public void run() {
								opHelper.toggleDial(true);

								father.hideProgress();
							}

						}.start();
					}
				});

		rootView.findViewById(R.id.button_pppoe_stop).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);
						new Thread() {
							@Override
							public void run() {
								opHelper.toggleDial(false);

								father.hideProgress();
							}

						}.start();
					}
				});

		rootView.findViewById(R.id.button_passwd_update).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);

						final String psw;

						if (checkBox_psw_override.isChecked()) {
							psw = editText_psw_override.getText().toString();
						} else {
							psw = father.mPSWOperator.getLastPsw(false);
						}

						new Thread() {
							@Override
							public void run() {
								opHelper.updatePSW(psw);

								father.hideProgress();
							}

						}.start();
					}
				});

		return rootView;
	}

	@Override
	public void onDestroy() {
		father.unregisterFragment(TAG);
		opHelper.shutdown();
		super.onDestroy();
	}

	public void updateStatus(int status) {
		int resId;
		switch (status) {
		case OpenWrtHelper.STATUS_ON:
			resId = R.drawable.status_on;
			break;
		case OpenWrtHelper.STATUS_OFF:
			resId = R.drawable.status_off;
			break;
		case OpenWrtHelper.STATUS_UNKNOWN:
		default:
			resId = R.drawable.status_unknown;
		}

		iv.setImageResource(resId);
	}
}
