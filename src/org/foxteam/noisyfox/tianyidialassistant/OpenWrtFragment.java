package org.foxteam.noisyfox.tianyidialassistant;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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

	public static final int STATUS_ON = 1;
	public static final int STATUS_OFF = 2;
	public static final int STATUS_UNKNOWN = 3;

	TyMainActivity father = null;
	SSHManager ssh = new SSHManager();
	String phoneNumber = "";
	String wanInterface = "wan";

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

	private void loadSSHSettings() {
		SharedPreferences defaultPreferences = PreferenceManager
				.getDefaultSharedPreferences(father);
		String ip, user, psw;
		int port;

		ip = defaultPreferences.getString("ssh_address", "192.168.1.1");
		port = Integer.parseInt(defaultPreferences.getString("ssh_port", "22"));
		user = defaultPreferences.getString("login_user", "root");
		psw = defaultPreferences.getString("login_passwd", "");
		wanInterface = defaultPreferences.getString("wan_interface", "wan");
		phoneNumber = defaultPreferences.getString("phone_number", "");

		ssh.setup(ip, port, user, psw);
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
		ssh.shutdown();
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

		updateStatus(STATUS_UNKNOWN);

		rootView.findViewById(R.id.button_check_status).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						loadSSHSettings();
						new Thread() {
							@Override
							public void run() {
								String[] results = ssh
										.exec("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH && ifconfig|grep pppoe");

								int status = STATUS_UNKNOWN;
								if (results != null) {
									for (String r : results)
										Log.d("result", r);
									if (results[0].isEmpty()) {
										status = STATUS_OFF;
									} else {
										status = STATUS_ON;
									}
								}

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
						loadSSHSettings();
						new Thread() {
							@Override
							public void run() {
								String[] results = ssh
										.exec("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH && ifup "
												+ wanInterface);
								if (results != null)
									for (String r : results)
										Log.d("result", r);

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
						loadSSHSettings();
						new Thread() {
							@Override
							public void run() {
								String[] results = ssh
										.exec("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH && ifdown "
												+ wanInterface);
								if (results != null)
									for (String r : results)
										Log.d("result", r);

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
						loadSSHSettings();

						String psw = null;

						if (checkBox_psw_override.isChecked()) {
							psw = editText_psw_override.getText().toString();
						} else {
							psw = father.mPSWOperator.getLastPsw(false);
						}

						if (phoneNumber.isEmpty()) {
							father.hideProgress();
							return;
						} else if (psw.isEmpty()) {
							father.hideProgress();
							return;
						}

						StringBuilder sb = new StringBuilder();
						sb.append("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH");
						sb.append(" && ");
						sb.append("uci set network.");
						sb.append(wanInterface);
						sb.append(".proto=pppoe");
						sb.append(" && ");
						sb.append("uci set network.");
						sb.append(wanInterface);
						sb.append(".username=^#01");
						sb.append(phoneNumber);
						sb.append(" && ");
						sb.append("uci set network.");
						sb.append(wanInterface);
						sb.append(".password=");
						sb.append(psw);
						sb.append(" && ");
						sb.append("uci set network.");
						sb.append(wanInterface);
						sb.append(".pppd_options='ty_dial'");
						sb.append("&&");
						sb.append("uci commit");

						final String cmd = sb.toString();
						new Thread() {
							@Override
							public void run() {
								String[] results = ssh.exec(cmd);
								if (results != null)
									for (String r : results)
										Log.d("result", r);

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
		ssh.shutdown();
		super.onDestroy();
	}

	public void updateStatus(int status) {
		int resId;
		switch (status) {
		case STATUS_ON:
			resId = R.drawable.status_on;
			break;
		case STATUS_OFF:
			resId = R.drawable.status_off;
			break;
		case STATUS_UNKNOWN:
		default:
			resId = R.drawable.status_unknown;
		}

		iv.setImageResource(resId);
	}
}
