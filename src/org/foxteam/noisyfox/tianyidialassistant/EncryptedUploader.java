package org.foxteam.noisyfox.tianyidialassistant;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.rt.BASE64Decoder;
import com.rt.BASE64Encoder;

/**
 * 负责配对、上传密码等与服务器交互的类
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

	private static final String RESULT_KEY_RESULT = "result";
	private static final String RESULT_KEY_KEY = "key";
	private static final String RESULT_KEY_KEYID = "key_id";
	private static final String RESULT_KEY_TIME = "time";

	private static final String STR_SERVER_URL_BASE = "http://192.168.0.13";
	private static final String STR_SERVER_URL_DOWNLOAD = STR_SERVER_URL_BASE
			+ "/public_key/download";
	private static final String STR_SERVER_URL_CHECK = STR_SERVER_URL_BASE
			+ "/public_key/check";
	private static final String STR_SERVER_URL_UPLOAD = STR_SERVER_URL_BASE
			+ "/password/upload";
	private static final String STR_SERVER_ARGS_KEY_KEYID = "key_id";
	private static final String STR_SERVER_ARGS_KEY_PAIRID = "pair_id";
	private static final String STR_SERVER_ARGS_KEY_PAYLOAD = "payload";
	private static final String STR_SERVER_ARGS_KEY_HASH = "hash";

	private String mErrMessage = null;

	// private final Context mContext;
	private SharedPreferences mPreferences = null;

	private long mKid = -1; // 公钥持久编号
	private PublicKey mPublicKey = null; // 加密用公钥
	private String mPublicKeyBase64 = null; // base64加密后公钥字符串

	private long mKid_pairing = -1; // 公钥持久编号--配对时
	private PublicKey mPublicKey_pairing = null; // 加密用公钥--配对时
	private String mPublicKeyBase64_pairing = null; // base64加密后公钥字符串--配对时

	public EncryptedUploader(Context context) {
		// mContext = context;

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

		save();
		
		return true;
	}

	/**
	 * 由公钥提取码提取公钥并获取配对密码
	 * 
	 * @param primaryPairingCode
	 *            公钥提取码
	 * @return 若提取码失效，返回null； 否则返回配对密码
	 */
	public String getSecondaryPairingCode(String primaryPairingCode) {
		mKid_pairing = -1;
		mPublicKey_pairing = null;
		mPublicKeyBase64_pairing = null;

		Map<String, String> queryStringMap = new HashMap<String, String>();
		queryStringMap.put(STR_SERVER_ARGS_KEY_PAIRID, primaryPairingCode);

		PostResult result = doHttpRequest(STR_SERVER_URL_DOWNLOAD,
				queryStringMap, null);

		switch (result.statusCode) {
		case HttpStatus.SC_OK:
			break;
		case HttpStatus.SC_UNAUTHORIZED:
			mErrMessage = "公钥编号不存在/已失效";
			return null;
		default:
			mErrMessage = "未知错误: " + result.statusCode;
			return null;
		}

		try {
			String keyBase64 = result.resultObj.getString(RESULT_KEY_KEY);
			long key_id = result.resultObj.getLong(RESULT_KEY_KEYID);
			double time = result.resultObj.getDouble(RESULT_KEY_TIME);
			PublicKey publicKey = getPublicKey(keyBase64);

			String totp = generateTOTP(publicKey.getEncoded(), time, 8,
					HMAC_SHA1);

			mKid_pairing = key_id;
			mPublicKey_pairing = publicKey;
			mPublicKeyBase64_pairing = keyBase64;

			return totp;
		} catch (Exception e) {
			e.printStackTrace();
			mErrMessage = "内部错误";
		}

		return null;
	}

	/**
	 * 验证是否完成配对，并保存信息
	 * 
	 * @return 是否配对成功
	 */
	public boolean finishPairing() {

		Map<String, String> queryStringMap = new HashMap<String, String>();
		queryStringMap.put(STR_SERVER_ARGS_KEY_KEYID,
				String.valueOf(mKid_pairing));

		checkLoop: while (true) {
			PostResult result = doHttpRequest(STR_SERVER_URL_CHECK,
					queryStringMap, null);

			switch (result.statusCode) {
			case HttpStatus.SC_OK:
				break;
			case HttpStatus.SC_UNAUTHORIZED:
				mErrMessage = "公钥无效/配对超时";
				return false;
			default:
				mErrMessage = "未知错误: " + result.statusCode;
				return false;
			}

			try {
				int pairResult = result.resultObj.getInt(RESULT_KEY_RESULT);
				if (pairResult == 1) {
					break checkLoop;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				mErrMessage = "内部错误";
				return false;
			}

			try {
				Thread.sleep(3 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// 配对完成
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
		
		if (mPublicKey != null){
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

		if (mPublicKeyBase64 == null || mKid == -1) {
			mErrMessage = "读取现有配对信息失败";
			return false;
		}

		PublicKey publicKey;
		try {
			publicKey = getPublicKey(mPublicKeyBase64);
		} catch (Exception e) {
			e.printStackTrace();
			mErrMessage = "读取现有配对信息失败";
			return false;
		}

		mKid = key_id;
		mPublicKey = publicKey;
		mPublicKeyBase64 = keyBase64;

		return true;
	}

	/**
	 * 上传动态密码
	 * 
	 * @param phoneNumber
	 *            手机号码
	 * @param passwd
	 *            动态密码
	 * @param getTime
	 *            密码获取时间
	 * @return 是否成功
	 */
	public boolean upload(String phoneNumber, String passwd, long getTime) {
		try {
			double time = (double) getTime / 1000.0;
			JSONObject payload = new JSONObject();
			payload.put(PAYLOAD_KEY_PHONENUMBER, phoneNumber);
			payload.put(PAYLOAD_KEY_PASSWD, passwd);
			payload.put(PAYLOAD_KEY_GETTIME, time);

			String payloadStr = payload.toString();

			byte[] payloadByteEncrypted = encryptByPublicKey(
					payloadStr.getBytes("ASCII"), mPublicKey);
			String payloadEncryptedBase64 = new BASE64Encoder()
					.encode(payloadByteEncrypted);
			String payloadEncryptHash = getSignature(payloadByteEncrypted,
					mPublicKey.getEncoded());
			Map<String, String> queryStringMap = new HashMap<String, String>();
			Map<String, String> dataMap = new HashMap<String, String>();

			queryStringMap.put(STR_SERVER_ARGS_KEY_KEYID, String.valueOf(mKid));
			dataMap.put(STR_SERVER_ARGS_KEY_PAYLOAD, payloadEncryptedBase64);
			dataMap.put(STR_SERVER_ARGS_KEY_HASH, payloadEncryptHash);

			PostResult result = doHttpRequest(STR_SERVER_URL_UPLOAD,
					queryStringMap, dataMap);

			switch (result.statusCode) {
			case HttpStatus.SC_OK:
				return true;
			case HttpStatus.SC_UNAUTHORIZED:
				mErrMessage = "公钥编号不存在/已失效";
				break;
			case HttpStatus.SC_FORBIDDEN:
				mErrMessage = "hash验证失败";
				break;
			default:
				mErrMessage = "未知错误: " + result.statusCode;
			}
		} catch (Exception e) {
			e.printStackTrace();
			mErrMessage = "内部错误";
		}

		return false;
	}

	/**
	 * 得到公钥
	 * 
	 * @param key
	 *            密钥字符串（经过base64编码）
	 * @return 获取失败返回null，否则返回公钥
	 */
	private static PublicKey getPublicKey(String key) throws Exception {
		byte[] keyBytes = (new BASE64Decoder()).decodeBuffer(key);

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(keySpec);

		return publicKey;
	}

	private static final String HMAC_SHA1 = "HmacSHA1";
	/**
	 * RSA最大加密明文大小
	 */
	private static final int MAX_ENCRYPT_BLOCK = 117;
	private static final int[] DIGITS_POWER
	// 0 1 2 3 4 5 6 7 8
	= { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * <p>
	 * 公钥加密
	 * </p>
	 * 
	 * @param data
	 *            源数据
	 * @param publicKey
	 *            公钥
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptByPublicKey(byte[] data, PublicKey publicKey)
			throws Exception {
		// 对数据加密
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		int inputLen = data.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] cache;
		int i = 0;
		// 对数据分段加密
		while (inputLen - offSet > 0) {
			if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
				cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
			} else {
				cache = cipher.doFinal(data, offSet, inputLen - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * MAX_ENCRYPT_BLOCK;
		}
		byte[] encryptedData = out.toByteArray();
		out.close();
		return encryptedData;
	}

	/**
	 * This method uses the JCE to provide the crypto algorithm. HMAC computes a
	 * Hashed Message Authentication Code with the crypto hash algorithm as a
	 * parameter.
	 * 
	 * @param crypto
	 *            : the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
	 * @param keyBytes
	 *            : the bytes to use for the HMAC key
	 * @param text
	 *            : the message or text to be authenticated
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	private static byte[] hmac_sha(String crypto, byte[] keyBytes, byte[] data)
			throws NoSuchAlgorithmException, InvalidKeyException {

		Mac hmac;
		hmac = Mac.getInstance(crypto);
		SecretKeySpec macKey = new SecretKeySpec(keyBytes, crypto);
		hmac.init(macKey);
		return hmac.doFinal(data);
	}

	/**
	 * 生成签名数据
	 * 
	 * @param data
	 *            待加密的数据
	 * @param key
	 *            加密使用的key
	 * @return 生成SHA1编码的字符串
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
	private static String getSignature(byte[] data, byte[] key)
			throws InvalidKeyException, NoSuchAlgorithmException {
		return encode("SHA1", hmac_sha(HMAC_SHA1, key, data));
	}

	/**
	 * encode string
	 * 
	 * @param algorithm
	 * @param str
	 * @return String
	 */
	private static String encode(String algorithm, byte[] data) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
			messageDigest.reset();
			messageDigest.update(data);
			return getFormattedText(messageDigest.digest());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String getFormattedText(byte[] bytes) {
		int len = bytes.length;
		StringBuilder buf = new StringBuilder(len * 2);
		// 把密文转换成十六进制的字符串形式
		for (int j = 0; j < len; j++) {
			buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
			buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
		}

		return buf.toString();
	}

	/**
	 * This method converts a HEX string to Byte[]
	 * 
	 * @param hex
	 *            : the HEX string
	 * 
	 * @return: a byte array
	 */
	private static byte[] hexStr2Bytes(String hex) {
		// Adding one byte to get the right conversion
		// Values starting with "0" can be converted
		byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

		// Copy all the REAL bytes, not the "first"
		byte[] ret = new byte[bArray.length - 1];
		for (int i = 0; i < ret.length; i++)
			ret[i] = bArray[i + 1];
		return ret;
	}

	/**
	 * This method generates a TOTP value for the given set of parameters.
	 * 
	 * @param key
	 *            : the shared secret, HEX encoded
	 * @param timeSeconds
	 *            : a value that reflects a time
	 * @param returnDigits
	 *            : number of digits to return
	 * @param crypto
	 *            : the crypto function to use
	 * 
	 * @return: a numeric String in base 10 that includes
	 *          {@link truncationDigits} digits
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	@SuppressLint("DefaultLocale")
	private static String generateTOTP(byte[] key, double timeSeconds,
			int codeDigits, String crypto) throws InvalidKeyException,
			NoSuchAlgorithmException {
		String result = null;

		// 转换时间为byte
		long timeMiS = (long) (timeSeconds * 1000);
		String time = Long.toHexString(timeMiS).toUpperCase();

		// Using the counter
		// First 8 bytes are for the movingFactor
		// Compliant with base RFC 4226 (HOTP)
		while (time.length() < 16)
			time = "0" + time;

		// Get the HEX in a Byte[]
		byte[] msg = hexStr2Bytes(time);
		byte[] hash = hmac_sha(crypto, key, msg);

		// put selected bytes into result int
		int offset = hash[hash.length - 1] & 0xf;

		int binary = ((hash[offset] & 0x7f) << 24)
				| ((hash[offset + 1] & 0xff) << 16)
				| ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

		int otp = binary % DIGITS_POWER[codeDigits];

		result = Integer.toString(otp);
		while (result.length() < codeDigits) {
			result = "0" + result;
		}
		return result;
	}

	private static class PostResult {
		int statusCode = 0;
		JSONObject resultObj = null;
	}

	private static PostResult doHttpRequest(String url,
			Map<String, String> queryString, Map<String, String> data) {

		PostResult result = new PostResult();
		result.statusCode = -1;
		try {

			DefaultHttpClient httpClient = new DefaultHttpClient();
			// Represents a collection of HTTP protocol and framework parameters
			HttpParams params = httpClient.getParams();
			// 设置超时
			HttpConnectionParams.setConnectionTimeout(params, 5000);
			HttpConnectionParams.setSoTimeout(params, 35000);

			if (queryString != null) {
				Set<Entry<String, String>> querys = queryString.entrySet();
				if (querys.size() > 0) {
					StringBuilder sb = new StringBuilder();
					sb.append(url);
					char separator = '?';
					for (Map.Entry<String, String> query : querys) {
						sb.append(separator);
						sb.append(query.getKey());
						sb.append('=');
						sb.append(query.getValue());
						separator = '&';
					}
					url = sb.toString();
				}
			}

			HttpPost post = new HttpPost(url);

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
				result.resultObj = new JSONObject(jsonStr);
			}

			result.statusCode = statusCode;
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
		}

		return result;
	}

}
