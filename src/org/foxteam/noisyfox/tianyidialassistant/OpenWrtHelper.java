package org.foxteam.noisyfox.tianyidialassistant;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.WeakHashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class OpenWrtHelper {
	public static final int STATUS_ON = 1;
	public static final int STATUS_OFF = 2;
	public static final int STATUS_UNKNOWN = 3;

	private static final long DELAY = 30000;

	private static DelayCloseHandler mHandler = new DelayCloseHandler();

	private static class DelayJob {
		final WeakReference<Context> mContext;
		final WeakReference<OpenWrtHelper> mHelper;

		public DelayJob(Context context, OpenWrtHelper helper) {
			mContext = new WeakReference<Context>(context);
			mHelper = new WeakReference<OpenWrtHelper>(helper);
		}
	}

	private static class DelayCloseHandler extends Handler {
		WeakHashMap<OpenWrtHelper, DelayJob> mJobs_wifi = new WeakHashMap<OpenWrtHelper, DelayJob>();
		WeakHashMap<OpenWrtHelper, DelayJob> mJobs_ssh = new WeakHashMap<OpenWrtHelper, DelayJob>();

		public synchronized void requireWifiJob(OpenWrtHelper helper,
				Context context) {
			DelayJob job = mJobs_wifi.get(helper);
			if (job != null) {
				removeMessages(0, job);
			}
			job = new DelayJob(context, helper);
			sendMessageDelayed(obtainMessage(0, job), DELAY);
			mJobs_wifi.put(helper, job);
		}

		public synchronized void requireSSHJob(OpenWrtHelper helper) {
			DelayJob job = mJobs_ssh.get(helper);
			if (job != null) {
				removeMessages(1, job);
			}
			job = new DelayJob(null, helper);
			sendMessageDelayed(obtainMessage(1, job), DELAY);
			mJobs_ssh.put(helper, job);
		}

		public synchronized void delayJob(OpenWrtHelper helper) {
			DelayJob job = mJobs_wifi.get(helper);
			if (job != null) {
				if (hasMessages(0, job)) {
					removeMessages(0, job);
					sendMessageDelayed(obtainMessage(0, job), DELAY);
					mJobs_wifi.put(helper, job);
				} else {
					mJobs_wifi.remove(helper);
				}
			}
			job = mJobs_ssh.get(helper);
			if (job != null) {
				if (hasMessages(1, job)) {
					removeMessages(1, job);
					sendMessageDelayed(obtainMessage(1, job), DELAY);
					mJobs_ssh.put(helper, job);
				} else {
					mJobs_ssh.remove(helper);
				}
			}
		}

		public synchronized void flushJob(OpenWrtHelper helper) {
			DelayJob job = mJobs_wifi.get(helper);
			if (job != null) {
				if (hasMessages(0, job)) {
					removeMessages(0, job);
					sendMessage(obtainMessage(0, job));
				}
				mJobs_wifi.remove(helper);
			}
			job = mJobs_ssh.get(helper);
			if (job != null) {
				if (hasMessages(1, job)) {
					removeMessages(1, job);
					sendMessage(obtainMessage(1, job));
				}
				mJobs_ssh.remove(helper);
			}
		}

		@Override
		public synchronized void handleMessage(Message msg) {
			if (msg.what == 0) {
				DelayJob job = (DelayJob) msg.obj;
				Context c = job.mContext.get();
				OpenWrtHelper helper = job.mHelper.get();
				if (helper == null)
					return;
				mJobs_wifi.remove(helper);
				if (c == null)
					return;
				helper.wifiAutoClose(c);
			} else if (msg.what == 1) {
				DelayJob job = (DelayJob) msg.obj;
				OpenWrtHelper helper = job.mHelper.get();
				if (helper == null)
					return;
				mJobs_ssh.remove(helper);
				helper.shutdown();
			}
		}

	}

	private SSHManager ssh = new SSHManager();
	public String phoneNumber = "";
	public String wanInterface = "wan";
	public String wifiSSID = "";
	public boolean wifiAutoConnect = false;

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
		wifiSSID = defaultPreferences.getString("wifi_ssid", "");
		wifiAutoConnect = defaultPreferences.getBoolean("wifi_auto_connect",
				false);

		ssh.setup(ip, port, user, psw);
	}

	public int checkDialStatus() {
		mHandler.delayJob(this);
		String[] results = ssh
				.exec("export PATH=/bin:/sbin:/usr/bin:/usr/sbin:$PATH && ifconfig|grep pppoe");

		mHandler.requireSSHJob(this);

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

		mHandler.delayJob(this);
		return status;
	}

	public boolean toggleDial(boolean on) {
		mHandler.delayJob(this);
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
		mHandler.requireSSHJob(this);
		if (results != null)
			for (String r : results)
				Log.d("result", r);
		mHandler.delayJob(this);

		return results != null;
	}

	public boolean updatePSW(String psw) {
		mHandler.delayJob(this);
		if (phoneNumber.isEmpty()) {
			return false;
		} else if (psw.isEmpty()) {
			return false;
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
		mHandler.requireSSHJob(this);
		if (results != null)
			for (String r : results)
				Log.d("result", r);
		mHandler.delayJob(this);

		return results != null;
	}

	public void shutdown() {
		ssh.shutdown();
	}

	public boolean checkWifiStatues(Context context, String ssid) {
		WifiManager wifiService = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (!wifiService.isWifiEnabled()) {
			return false;
		}

		WifiInfo wifiInfo = wifiService.getConnectionInfo();

		if (ssid == null) {
			return wifiInfo.getSSID() != null;
		} else {
			return ssid.equals(wifiInfo.getSSID());
		}
	}

	public boolean wifiAutoConnect(Context context) {
		WifiManager wifiService = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (!wifiService.setWifiEnabled(true)) {
			return false;
		}

		int failCount = 0;

		while (wifiService.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
			if (failCount > 5) {
				return false;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			failCount++;
		}

		List<WifiConfiguration> wcs = wifiService.getConfiguredNetworks();
		if (wcs == null) {
			return false;
		}

		WifiConfiguration fwc = null;

		for (WifiConfiguration wc : wcs) {
			if (wifiSSID.equals(wc.SSID)) {
				fwc = wc;
				break;
			}
		}

		if (fwc == null) {
			return false;
		}

		if (!wifiService.enableNetwork(fwc.networkId, true)) {
			return false;
		}

		failCount = 0;
		for (String ssid = wifiService.getConnectionInfo().getSSID(); ssid == null
				|| !(wifiSSID.equals(ssid) || wifiSSID.equals("\"" + ssid
						+ "\"")); ssid = wifiService.getConnectionInfo()
				.getSSID()) {
			if (failCount > 5) {
				return false;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			failCount++;
		}

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		return true;
	}

	public boolean wifiAutoClose(Context context) {
		ssh.shutdown();
		WifiManager wifiService = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (!wifiService.disconnect()) {
			return false;
		}

		return wifiService.setWifiEnabled(false);
	}

	public boolean beforeAction(Context context) {
		mHandler.delayJob(this);
		boolean needOpen = wifiAutoConnect
				&& (!checkWifiStatues(context, null));
		if (needOpen && wifiAutoConnect(context)) {
			return true;
		}
		return false;
	}

	public boolean afterAction(Context context, boolean needWifiClose,
			boolean delayed) {
		if (needWifiClose) {
			if (!delayed) {
				return wifiAutoClose(context);
			} else {
				mHandler.requireWifiJob(this, context);
			}
		}
		return true;
	}

	public void flushAll() {
		mHandler.flushJob(this);
	}

}
