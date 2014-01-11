package org.foxteam.noisyfox.tianyidialassistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.foxteam.noisyfox.tianyidialassistant.NetworkHelper.PostResult;
import org.foxteam.noisyfox.tianyidialassistant.NetworkHelper.StaticHost;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.rt.BASE64Decoder;
import com.rt.BASE64Encoder;

/**
 * ������ԡ��ϴ���������������������
 * 
 * @author Noisyfox
 */
public final class EncryptedUploader {
	private static final String SP_NAME = "Encrypt";
	private static final String SP_VALUE_STR_PUBLICKEY = "PublicKey";
	private static final String SP_VALUE_LONG_KEYID = "KeyID";

	private static final String PAYLOAD_KEY_PHONENUMBER = "number";
	private static final String PAYLOAD_KEY_PASSWD = "password";
	private static final String PAYLOAD_KEY_GETTIME = "time";

	private static final String RESULT_KEY_ERROR_CODE = "error_code";
	private static final String RESULT_KEY_RESULT = "result";
	private static final String RESULT_KEY_KEY = "key";
	private static final String RESULT_KEY_KEYID = "key_id";
	private static final String RESULT_KEY_TIME = "time";

	private static final StaticHost HOST_SERVER = new StaticHost("https",
			"noisyfoxtest.appspot.com", 443, "www.google.com");

	private static final String STR_SERVER_PATH_DOWNLOAD = "/public_key/download";
	private static final String STR_SERVER_PATH_CHECK = "/public_key/check";
	private static final String STR_SERVER_PATH_UPLOAD = "/password/upload";

	private static final String STR_SERVER_ARGS_KEY_KEYID = "key_id";
	private static final String STR_SERVER_ARGS_KEY_PAIRID = "pair_id";
	private static final String STR_SERVER_ARGS_KEY_PAYLOAD = "payload";
	private static final String STR_SERVER_ARGS_KEY_HASH = "hash";

	private String mErrMessage = null;

	private SharedPreferences mPreferences = null;

	private long mKid = -1; // ��Կ�־ñ��
	private PublicKey mPublicKey = null; // �����ù�Կ
	private String mPublicKeyBase64 = null; // base64���ܺ�Կ�ַ���

	private long mKid_pairing = -1; // ��Կ�־ñ��--���ʱ
	private PublicKey mPublicKey_pairing = null; // �����ù�Կ--���ʱ
	private String mPublicKeyBase64_pairing = null; // base64���ܺ�Կ�ַ���--���ʱ

	public EncryptedUploader(Context context) {
		mPreferences = context.getApplicationContext().getSharedPreferences(
				SP_NAME, Context.MODE_PRIVATE);

		load();
	}

	public boolean isPaired() {
		return mPublicKey != null;
	}

	public String checkLastErrorMessage() {
		String err = mErrMessage;
		mErrMessage = null;

		return err;
	}

	public boolean removePairing() {
		mKid_pairing = -1;
		mPublicKey_pairing = null;
		mPublicKeyBase64_pairing = null;

		mKid = -1;
		mPublicKey = null;
		mPublicKeyBase64 = null;

		save();

		return true;
	}

	/**
	 * �ɹ�Կ��ȡ����ȡ��Կ����ȡ�������
	 * 
	 * @param primaryPairingCode
	 *            ��Կ��ȡ��
	 * @return ����ȡ��ʧЧ������null�� ���򷵻��������
	 */
	public String getSecondaryPairingCode(String primaryPairingCode) {
		mKid_pairing = -1;
		mPublicKey_pairing = null;
		mPublicKeyBase64_pairing = null;

		Map<String, String> queryStringMap = new HashMap<String, String>();
		queryStringMap.put(STR_SERVER_ARGS_KEY_PAIRID, primaryPairingCode);

		PostResult result = doHttpRequest(HOST_SERVER,
				STR_SERVER_PATH_DOWNLOAD, queryStringMap, null);

		switch (result.statusCode) {
		case HttpStatus.SC_OK:
			break;
		case HttpStatus.SC_UNAUTHORIZED:
			mErrMessage = "����벻����/��ʧЧ";
			return null;
		default:
			mErrMessage = "δ֪����: " + result.statusCode;
			return null;
		}

		try {
			String keyBase64 = result.resultObj.getString(RESULT_KEY_KEY);
			long key_id = result.resultObj.getLong(RESULT_KEY_KEYID);
			double time = result.resultObj.getDouble(RESULT_KEY_TIME);
			PublicKey publicKey = Encrypt.getPublicKey(keyBase64);
			byte[] key = (new BASE64Decoder()).decodeBuffer(keyBase64);

			String totp = Encrypt.generateTOTP(key, time, 6, Encrypt.HMAC_SHA1);

			mKid_pairing = key_id;
			mPublicKey_pairing = publicKey;
			mPublicKeyBase64_pairing = keyBase64;

			return totp;
		} catch (Exception e) {
			e.printStackTrace();
			mErrMessage = "�ڲ�����";
		}

		return null;
	}

	/**
	 * ��֤�Ƿ������ԣ���������Ϣ
	 * 
	 * @return �Ƿ���Գɹ�
	 */
	public boolean finishPairing() {

		Map<String, String> queryStringMap = new HashMap<String, String>();
		queryStringMap.put(STR_SERVER_ARGS_KEY_KEYID,
				String.valueOf(mKid_pairing));

		checkLoop: while (true) {

			try {
				Thread.sleep(3 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			PostResult result = doHttpRequest(HOST_SERVER,
					STR_SERVER_PATH_CHECK, queryStringMap, null);

			switch (result.statusCode) {
			case HttpStatus.SC_OK:
				break;
			case HttpStatus.SC_UNAUTHORIZED:
				mErrMessage = "�������Ч/��Գ�ʱ";
				return false;
			case -1:
				continue;
			default:
				mErrMessage = "δ֪����: " + result.statusCode;
				return false;
			}

			try {
				int pairResult = result.resultObj.getInt(RESULT_KEY_RESULT);
				if (pairResult == 1) {
					break checkLoop;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				mErrMessage = "�ڲ�����";
				return false;
			}
		}

		// ������
		mKid = mKid_pairing;
		mPublicKey = mPublicKey_pairing;
		mPublicKeyBase64 = mPublicKeyBase64_pairing;

		mKid_pairing = -1;
		mPublicKey_pairing = null;
		mPublicKeyBase64_pairing = null;

		save();
		return true;
	}

	public void save() {
		Editor e = mPreferences.edit();
		e.clear();

		if (mPublicKey != null) {
			e.putLong(SP_VALUE_LONG_KEYID, mKid);
			e.putString(SP_VALUE_STR_PUBLICKEY, mPublicKeyBase64);
		}

		e.commit();
	}

	public boolean load() {
		mKid = -1;
		mPublicKey = null;
		mPublicKeyBase64 = null;

		long key_id = mPreferences.getLong(SP_VALUE_LONG_KEYID, -1);
		String keyBase64 = mPreferences.getString(SP_VALUE_STR_PUBLICKEY, null);

		if (keyBase64 == null || key_id == -1) {
			mErrMessage = "��ȡ���������Ϣʧ��";
			return false;
		}

		PublicKey publicKey;
		try {
			publicKey = Encrypt.getPublicKey(keyBase64);
		} catch (Exception e) {
			e.printStackTrace();
			mErrMessage = "��ȡ���������Ϣʧ��";
			return false;
		}

		mKid = key_id;
		mPublicKey = publicKey;
		mPublicKeyBase64 = keyBase64;

		return true;
	}

	/**
	 * �ϴ���̬����
	 * 
	 * @param phoneNumber
	 *            �ֻ�����
	 * @param passwd
	 *            ��̬����
	 * @param getTime
	 *            �����ȡʱ��
	 * @return �Ƿ�ɹ�
	 */
	public boolean upload(String phoneNumber, String passwd, long getTime) {
		try {
			double time = (double) getTime / 1000.0;
			JSONObject payload = new JSONObject();
			payload.put(PAYLOAD_KEY_PHONENUMBER, phoneNumber);
			payload.put(PAYLOAD_KEY_PASSWD, passwd);
			payload.put(PAYLOAD_KEY_GETTIME, time);

			String payloadStr = payload.toString();

			byte[] payloadByteEncrypted = Encrypt.encryptByPublicKey(
					payloadStr.getBytes("ASCII"), mPublicKey);
			String payloadEncryptedBase64 = new BASE64Encoder()
					.encode(payloadByteEncrypted);
			String payloadEncryptHash = Encrypt.getSignature(
					payloadByteEncrypted, mPublicKey.getEncoded());
			Map<String, String> queryStringMap = new HashMap<String, String>();
			Map<String, String> dataMap = new HashMap<String, String>();

			queryStringMap.put(STR_SERVER_ARGS_KEY_KEYID, String.valueOf(mKid));
			dataMap.put(STR_SERVER_ARGS_KEY_PAYLOAD, payloadEncryptedBase64);
			dataMap.put(STR_SERVER_ARGS_KEY_HASH, payloadEncryptHash);

			PostResult result = doHttpRequest(HOST_SERVER,
					STR_SERVER_PATH_UPLOAD, queryStringMap, dataMap);

			switch (result.statusCode) {
			case HttpStatus.SC_OK:
				return true;
			case HttpStatus.SC_UNAUTHORIZED:
				mErrMessage = "����벻����/��ʧЧ";
				break;
			case HttpStatus.SC_FORBIDDEN:
				mErrMessage = "hash��֤ʧ��";
				break;
			default:
				mErrMessage = "δ֪����: " + result.statusCode;
			}
		} catch (Exception e) {
			e.printStackTrace();
			mErrMessage = "�ڲ�����";
		}

		return false;
	}

	private static PostResult doHttpRequest(StaticHost host, String path,
			Map<String, String> queryString, Map<String, String> data) {

		if (!NetworkHelper.resolveHost(host)) {
			Log.d("Resolve", "Resolve host failed.");
		}

		PostResult result = new PostResult();
		result.statusCode = -1;
		try {

			HttpClient httpClient = NetworkHelper.getNewHttpClient();// ��ȡһ��û��SSL֤���host��֤��HttpClient
			// Represents a collection of HTTP protocol and framework parameters
			HttpParams params = httpClient.getParams();
			// ���ó�ʱ
			HttpConnectionParams.setConnectionTimeout(params, 10000);
			HttpConnectionParams.setSoTimeout(params, 35000);

			String queryStr = "";

			if (queryString != null) {
				Set<Entry<String, String>> querys = queryString.entrySet();
				if (querys.size() > 0) {
					StringBuilder sb = new StringBuilder();
					for (Map.Entry<String, String> query : querys) {
						sb.append('&');
						sb.append(query.getKey());
						sb.append('=');
						sb.append(query.getValue());
					}
					queryStr = sb.substring(1);
				}
			}

			URI uri = URIUtils.createURI(host.scheme, host.host, host.port,
					path, queryStr, null);
			Log.d("URI", uri.toString());
			HttpPost post = new HttpPost(uri);
			// post.addHeader(HTTP.TARGET_HOST, host.host);

			if (data != null) {
				List<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>();
				for (Map.Entry<String, String> entry : data.entrySet()) {
					postData.add(new BasicNameValuePair(entry.getKey(), entry
							.getValue()));
				}
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(
						postData, HTTP.ASCII);
				post.setEntity(entity);
			}

			HttpResponse response = httpClient.execute(post);

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_OK) {
				HttpEntity httpEntity = response.getEntity();
				InputStream is = httpEntity.getContent();
				StringBuffer sb = new StringBuffer();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				String line = "";
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}

				String jsonStr = sb.toString();
				Log.d("Result", jsonStr);
				result.resultObj = new JSONObject(jsonStr);

				int serverStatus = result.resultObj
						.getInt(RESULT_KEY_ERROR_CODE);
				result.statusCode = serverStatus == 0 ? HttpStatus.SC_OK
						: serverStatus;
			} else {
				result.statusCode = statusCode;
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}
