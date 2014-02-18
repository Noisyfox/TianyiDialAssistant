package org.foxteam.noisyfox.tianyidialassistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SSHManager {
	private String mAddress;
	private int mPort;
	private String mUser;
	private String mPsw;

	private Connection conn = null;
	private Session sess = null;
	InputStream stdout = null;
	InputStream stderr = null;
	BufferedReader brout = null;
	BufferedReader brerr = null;

	public synchronized void setup(String address, int port, String user,
			String psw) {
		if (mAddress != address || mPort != port || mUser != user
				|| mPsw != psw) {
			shutdown();

			mAddress = address;
			mPort = port;
			mUser = user;
			mPsw = psw;
		}
	}

	public synchronized void shutdown() {
		if (conn == null)
			return;

		Log.d("SSH", "SSH shutdown!");

		closeSession();

		conn.close();

		conn = null;
	}

	void startSession() throws IOException {
		sess = conn.openSession();
		stdout = new StreamGobbler(sess.getStdout());
		stderr = new StreamGobbler(sess.getStderr());
		brout = new BufferedReader(new InputStreamReader(stdout));
		brerr = new BufferedReader(new InputStreamReader(stderr));
	}

	void closeSession() {
		if (sess != null)
			sess.close();

		Util.safeClose(brout);
		Util.safeClose(brerr);

		sess = null;
		stdout = null;
		stderr = null;
		brout = null;
		brerr = null;
	}

	public synchronized String[] exec(String... cmds) {
		try {
			if (conn == null) {
				conn = new Connection(mAddress, mPort);
				conn.connect();
				boolean isAuthenticated = conn.authenticateWithPassword(mUser,
						mPsw);

				if (!isAuthenticated)
					throw new IOException("Authentication failed.");

			}

			String[] results = new String[cmds.length];
			for (int i = 0; i < cmds.length; i++) {

				startSession();

				sess.execCommand(cmds[i]);
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = brout.readLine()) != null) {
					sb.append('\n');
					sb.append(line);
				}
				results[i] = sb.length() == 0 ? "" : sb.substring(1);

				while ((line = brerr.readLine()) != null) {
					Log.d("SSHErr", line);
				}

				closeSession();
			}

			return results;
		} catch (IOException e) {
			e.printStackTrace();
			shutdown();
			return null;
		}
	}

	// private class
}
