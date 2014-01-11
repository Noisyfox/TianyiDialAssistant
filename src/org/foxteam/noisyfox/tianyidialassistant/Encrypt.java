package org.foxteam.noisyfox.tianyidialassistant;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;

import com.rt.BASE64Decoder;

public class Encrypt {
	public static final String HMAC_SHA1 = "HmacSHA1";
	
	/**
	 * RSA���������Ĵ�С
	 */
	private static final int MAX_ENCRYPT_BLOCK = 117;
	private static final int[] DIGITS_POWER
	// 0 1 2 3 4 5 6 7 8
	= { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * �õ���Կ
	 * 
	 * @param key
	 *            ��Կ�ַ���������base64���룩
	 * @return ��ȡʧ�ܷ���null�����򷵻ع�Կ
	 */
	public static PublicKey getPublicKey(String key) throws Exception {
		byte[] keyBytes = (new BASE64Decoder()).decodeBuffer(key);

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(keySpec);

		return publicKey;
	}
	
	/**
	 * <p>
	 * ��Կ����
	 * </p>
	 * 
	 * @param data
	 *            Դ����
	 * @param publicKey
	 *            ��Կ
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptByPublicKey(byte[] data, PublicKey publicKey)
			throws Exception {
		// �����ݼ���
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		int inputLen = data.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] cache;
		int i = 0;
		// �����ݷֶμ���
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
	 * ����ǩ������
	 * 
	 * @param data
	 *            �����ܵ�����
	 * @param key
	 *            ����ʹ�õ�key
	 * @return ����SHA1������ַ���
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
	public static String getSignature(byte[] data, byte[] key)
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
		// ������ת����ʮ�����Ƶ��ַ�����ʽ
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
	public static String generateTOTP(byte[] key, double timeSeconds,
			int codeDigits, String crypto) throws InvalidKeyException,
			NoSuchAlgorithmException {
		String result = null;

		// ת��ʱ��Ϊbyte
		long timeMiS = (long) (timeSeconds / 30);
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
}
