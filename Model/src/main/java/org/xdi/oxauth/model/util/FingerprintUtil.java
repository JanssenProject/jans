package org.xdi.oxauth.model.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Movchan
 * @version March 03, 2016
 */
public class FingerprintUtil {

	private static final Logger log = Logger.getLogger(FingerprintUtil.class);

	/*
	 * Return SSH RSA public key fingerprint
	 */
	public static Object getPublicKeyFingerprint(PublicKey publicKey) throws NoSuchAlgorithmException, IOException {
		if (publicKey instanceof RSAPublicKey) {
			return getPublicKeyFingerprint((RSAPublicKey) publicKey);
		}
		
		throw new NoSuchAlgorithmException("Unsopported PublicKey type");
	}

	public static String getPublicKeyFingerprint(RSAPublicKey publicKey) throws NoSuchAlgorithmException, IOException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		
		byte[] derEncoded = getDerEncoding(publicKey);
		byte[] fingerprint = digest.digest(derEncoded);

		return Hex.encodeHexString(fingerprint);
	}

	private static byte[] getDerEncoding(RSAPublicKey key) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream dataOutput = new DataOutputStream(buffer);
		writeVariableLengthOpaque("ssh-rsa".getBytes(), dataOutput);
		writeVariableLengthOpaque(key.getPublicExponent().toByteArray(), dataOutput);
		writeVariableLengthOpaque(key.getModulus().toByteArray(), dataOutput);

		return buffer.toByteArray();
	}

	private static void writeVariableLengthOpaque(byte[] opaque, DataOutput byteBuffer) throws IOException {
		byteBuffer.writeInt(opaque.length);
		byteBuffer.write(opaque);
	}

}
