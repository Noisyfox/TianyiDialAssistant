package org.foxteam.noisyfox.tianyidialassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class OpenWrtHelper {
	public static final int STATUS_ON = 1;
	public static final int STATUS_OFF = 2;
	public static final int STATUS_UNKNOWN = 3;

	SSHManager ssh = new SSHManager();
	String phoneNumber = "";
	String wanInterface = "wan";

	public void loadSSHSettings(Context context) {
		SharedPreferences defaultPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
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

	public int checkDialStatus() {
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

		return status;
	}

	public void toggleDial(boolean on) {
		String[] results = null;
		if (on) {
			results = ssh
					.exec("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH && ifup "
							+ wanInterface);
		} else {
			results = ssh
					.exec("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH && ifdown "
							+ wanInterface);
		}
		if (results != null)
			for (String r : results)
				Log.d("result", r);
	}

	public void updatePSW(String psw) {
		if (phoneNumber.isEmpty()) {
			return;
		} else if (psw.isEmpty()) {
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

		String[] results = ssh.exec(cmd);
		if (results != null)
			for (String r : results)
				Log.d("result", r);
	}

	public void shutdown() {
		ssh.shutdown();
	}

}
