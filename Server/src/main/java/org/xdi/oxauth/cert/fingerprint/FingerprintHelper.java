/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.cert.fingerprint;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility which help to calculate SSH RSA public key fingerprint
 * 
 * @author Yuriy Movchan
 * @version March 03, 2016
 */
public class FingerprintHelper {

	private static final Logger log = LoggerFactory.getLogger(FingerprintHelper.class);

	/*
	 * Return SSH RSA public key fingerprint
	 */
	public static String getPublicKeySshFingerprint(PublicKey publicKey) throws NoSuchAlgorithmException, IOException {
		if (publicKey instanceof RSAPublicKey) {
			return getPublicKeySshFingerprint((RSAPublicKey) publicKey);
		}

		throw new NoSuchAlgorithmException("Unsopported PublicKey type");
	}

	public static String getPublicKeySshFingerprint(RSAPublicKey publicKey) throws NoSuchAlgorithmException, IOException {
		MessageDigest digest = MessageDigest.getInstance("MD5");

		byte[] derEncoded = getDerEncoding(publicKey);
		byte[] fingerprint = digest.digest(derEncoded);

		return Hex.encodeHexString(fingerprint);
	}

	private static byte[] getDerEncoding(RSAPublicKey key) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream dataOutput = new DataOutputStream(buffer);
		writeDataWithLength("ssh-rsa".getBytes(), dataOutput);
		writeDataWithLength(key.getPublicExponent().toByteArray(), dataOutput);
		writeDataWithLength(key.getModulus().toByteArray(), dataOutput);

		return buffer.toByteArray();
	}

	private static void writeDataWithLength(byte[] data, DataOutput byteBuffer) throws IOException {
		byteBuffer.writeInt(data.length);
		byteBuffer.write(data);
	}

}
