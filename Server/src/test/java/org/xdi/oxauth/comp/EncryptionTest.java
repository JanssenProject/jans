/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.xdi.oxauth.BaseTest;

/**
 * @author Javier Rojas Blum
 * @version August 17, 2016
 */
public class EncryptionTest extends BaseTest {

	/*
	@Test
	public void cryptoTest1() throws Exception {
		showTitle("Test: alg = RSA-OAEP, enc = A256GCM");

		// {"alg":"RSA-OAEP","enc":"A256GCM"}
		String encodedHeader = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00ifQ";
		String plainText = "Live long and prosper.";
		byte[] cmk = Base64Util.unsignedToBytes(new int[] { 177, 161, 244, 128, 84, 143, 225, 115, 63, 180, 3, 255, 107,
				154, 212, 246, 138, 7, 110, 91, 112, 46, 34, 105, 47, 130, 203, 46, 122, 234, 64, 252 });

		BigInteger modulus = new BigInteger(1,
				Base64Util.unsignedToBytes(new int[] { 161, 168, 84, 34, 133, 176, 208, 173, 46, 176, 163, 110, 57, 30,
						135, 227, 9, 31, 226, 128, 84, 92, 116, 241, 70, 248, 27, 227, 193, 62, 5, 91, 241, 145, 224,
						205, 141, 176, 184, 133, 239, 43, 81, 103, 9, 161, 153, 157, 179, 104, 123, 51, 189, 34, 152,
						69, 97, 69, 78, 93, 140, 131, 87, 182, 169, 101, 92, 142, 3, 22, 167, 8, 212, 56, 35, 79, 210,
						222, 192, 208, 252, 49, 109, 138, 173, 253, 210, 166, 201, 63, 102, 74, 5, 158, 41, 90, 144,
						108, 160, 79, 10, 89, 222, 231, 172, 31, 227, 197, 0, 19, 72, 81, 138, 78, 136, 221, 121, 118,
						196, 17, 146, 10, 244, 188, 72, 113, 55, 221, 162, 217, 171, 27, 57, 233, 210, 101, 236, 154,
						199, 56, 138, 239, 101, 48, 198, 186, 202, 160, 76, 111, 234, 71, 57, 183, 5, 211, 171, 136,
						126, 64, 40, 75, 58, 89, 244, 254, 107, 84, 103, 7, 236, 69, 163, 18, 180, 251, 58, 153, 46,
						151, 174, 12, 103, 197, 181, 161, 162, 55, 250, 235, 123, 110, 17, 11, 158, 24, 47, 133, 8, 199,
						235, 107, 126, 130, 246, 73, 195, 20, 108, 202, 176, 214, 187, 45, 146, 182, 118, 54, 32, 200,
						61, 201, 71, 243, 1, 255, 131, 84, 37, 111, 211, 168, 228, 45, 192, 118, 27, 197, 235, 232, 36,
						10, 230, 248, 190, 82, 182, 140, 35, 204, 108, 190, 253, 186, 186, 27 }));

		BigInteger exponent = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 1, 0, 1 }));

		BigInteger privateExponent = new BigInteger(1,
				Base64Util.unsignedToBytes(new int[] { 144, 183, 109, 34, 62, 134, 108, 57, 44, 252, 10, 66, 73, 54, 16,
						181, 233, 92, 54, 219, 101, 42, 35, 178, 63, 51, 43, 92, 119, 136, 251, 41, 53, 23, 191, 164,
						164, 60, 88, 227, 229, 152, 228, 213, 149, 228, 169, 237, 104, 71, 151, 75, 88, 252, 216, 77,
						251, 231, 28, 97, 88, 193, 215, 202, 248, 216, 121, 195, 211, 245, 250, 112, 71, 243, 61, 129,
						95, 39, 244, 122, 225, 217, 169, 211, 165, 48, 253, 220, 59, 122, 219, 42, 86, 223, 32, 236, 39,
						48, 103, 78, 122, 216, 187, 88, 176, 89, 24, 1, 42, 177, 24, 99, 142, 170, 1, 146, 43, 3, 108,
						64, 194, 121, 182, 95, 187, 134, 71, 88, 96, 134, 74, 131, 167, 69, 106, 143, 121, 27, 72, 44,
						245, 95, 39, 194, 179, 175, 203, 122, 16, 112, 183, 17, 200, 202, 31, 17, 138, 156, 184, 210,
						157, 184, 154, 131, 128, 110, 12, 85, 195, 122, 241, 79, 251, 229, 183, 117, 21, 123, 133, 142,
						220, 153, 9, 59, 57, 105, 81, 255, 138, 77, 82, 54, 62, 216, 38, 249, 208, 17, 197, 49, 45, 19,
						232, 157, 251, 131, 137, 175, 72, 126, 43, 229, 69, 179, 117, 82, 157, 213, 83, 35, 57, 210,
						197, 252, 171, 143, 194, 11, 47, 163, 6, 253, 75, 252, 96, 11, 187, 84, 130, 210, 7, 121, 78,
						91, 79, 57, 251, 138, 132, 220, 60, 224, 173, 56, 224, 201 }));

		PublicKey publicKey = new RSAPublicKeyImpl(modulus, exponent);
		RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(modulus, privateExponent);

		// Encrypt
		JweEncrypterImpl encrypter = new JweEncrypterImpl(KeyEncryptionAlgorithm.RSA_OAEP,
				BlockEncryptionAlgorithm.A256GCM, publicKey);
		String encodedEncryptedKey = encrypter.generateEncryptedKey(cmk);

		byte[] initVector = Base64Util
				.unsignedToBytes(new int[] { 227, 197, 117, 252, 2, 219, 233, 68, 180, 225, 77, 219 });
		String encodedInitVector = Base64Util.base64urlencode(initVector);
		assertEquals(encodedInitVector, "48V1_ALb6US04U3b");

		String additionalAuthenticatedData = encodedHeader + "." + encodedEncryptedKey + "." + encodedInitVector;

		Pair<String, String> ciphertextAndIntegrityValue = encrypter.generateCipherTextAndIntegrityValue(cmk,
				initVector, additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING),
				plainText.getBytes(Util.UTF8_STRING_ENCODING));
		String encodedCipherText = ciphertextAndIntegrityValue.getFirst();
		String encodedAuthenticationTag = ciphertextAndIntegrityValue.getSecond();

		String encodedJwe = encodedHeader + "." + encodedEncryptedKey + "." + encodedInitVector + "."
				+ encodedCipherText + "." + encodedAuthenticationTag;
		System.out.println("JWE: " + encodedJwe);

		// Decrypt
		JweDecrypterImpl decrypter = new JweDecrypterImpl(rsaPrivateKey);
		decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.RSA_OAEP);
		decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A256GCM);
		byte[] encryptionKey = decrypter.decryptEncryptionKey(encodedEncryptedKey);
		assertEquals(encryptionKey, cmk);

		String decodedPlainText = decrypter.decryptCipherText(encodedCipherText, encryptionKey, initVector,
				Base64Util.base64urldecode(encodedAuthenticationTag),
				additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING));
		assertEquals(decodedPlainText, plainText);
	}

	@Test
	public void cryptoTest2() throws Exception {
		showTitle("Test: alg = RSA1_5, enc = A128CBC+HS256");

		// {"alg":"RSA1_5","enc":"A128CBC+HS256"}
		String encodedHeader = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDK0hTMjU2In0";
		String plainText = "No matter where you go, there you are.";
		byte[] cmk = Base64Util.unsignedToBytes(new int[] { 4, 211, 31, 197, 84, 157, 252, 254, 11, 100, 157, 250, 63,
				170, 106, 206, 107, 124, 212, 45, 111, 107, 9, 219, 200, 177, 0, 240, 143, 156, 44, 207 });

		BigInteger modulus = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 177, 119, 33, 13, 164, 30, 108,
				121, 207, 136, 107, 242, 12, 224, 19, 226, 198, 134, 17, 71, 173, 75, 42, 61, 48, 162, 206, 161, 97,
				108, 185, 234, 226, 219, 118, 206, 118, 5, 169, 224, 60, 181, 90, 85, 51, 123, 6, 224, 4, 122, 29, 230,
				151, 12, 244, 127, 121, 25, 4, 85, 220, 144, 215, 110, 130, 17, 68, 228, 129, 138, 7, 130, 231, 40, 212,
				214, 17, 179, 28, 124, 151, 178, 207, 20, 14, 154, 222, 113, 176, 24, 198, 73, 211, 113, 9, 33, 178, 80,
				13, 25, 21, 25, 153, 212, 206, 67, 154, 147, 70, 194, 192, 183, 160, 83, 98, 236, 175, 85, 23, 97, 75,
				199, 177, 73, 145, 50, 253, 206, 32, 179, 254, 236, 190, 82, 73, 67, 129, 253, 252, 220, 108, 136, 138,
				11, 192, 1, 36, 239, 228, 55, 81, 113, 17, 25, 140, 63, 239, 146, 3, 172, 96, 60, 227, 233, 64, 255,
				224, 173, 225, 228, 229, 92, 112, 72, 99, 97, 26, 87, 187, 123, 46, 50, 90, 202, 117, 73, 10, 153, 47,
				224, 178, 163, 77, 48, 46, 154, 33, 148, 34, 228, 33, 172, 216, 89, 46, 225, 127, 68, 146, 234, 30, 147,
				54, 146, 5, 133, 45, 78, 254, 85, 55, 75, 213, 86, 194, 218, 215, 163, 189, 194, 54, 6, 83, 36, 18, 153,
				53, 7, 48, 89, 35, 66, 144, 7, 65, 154, 13, 97, 75, 55, 230, 132, 3, 13, 239, 71 }));

		BigInteger exponent = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 1, 0, 1 }));

		BigInteger privateExponent = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 84, 80, 150, 58, 165, 235,
				242, 123, 217, 55, 38, 154, 36, 181, 221, 156, 211, 215, 100, 164, 90, 88, 40, 228, 83, 148, 54, 122, 4,
				16, 165, 48, 76, 194, 26, 107, 51, 53, 179, 165, 31, 18, 198, 173, 78, 61, 56, 97, 252, 158, 140, 80,
				63, 25, 223, 156, 36, 203, 214, 252, 120, 67, 180, 167, 3, 82, 243, 25, 97, 214, 83, 133, 69, 16, 104,
				54, 160, 200, 41, 83, 164, 187, 70, 153, 111, 234, 242, 158, 175, 28, 198, 48, 211, 45, 148, 58, 23, 62,
				227, 74, 52, 117, 42, 90, 41, 249, 130, 154, 80, 119, 61, 26, 193, 40, 125, 10, 152, 174, 227, 225, 205,
				32, 62, 66, 6, 163, 100, 99, 219, 19, 253, 25, 105, 80, 201, 29, 252, 157, 237, 69, 1, 80, 171, 167, 20,
				196, 156, 109, 249, 88, 0, 3, 152, 38, 165, 72, 87, 6, 152, 71, 156, 214, 16, 71, 30, 82, 51, 103, 76,
				218, 63, 9, 84, 163, 249, 91, 215, 44, 238, 85, 101, 240, 148, 1, 82, 224, 91, 135, 105, 127, 84, 171,
				181, 152, 210, 183, 126, 24, 46, 196, 90, 173, 38, 245, 219, 186, 222, 27, 240, 212, 194, 15, 66, 135,
				226, 178, 190, 52, 245, 74, 65, 224, 81, 100, 85, 25, 204, 165, 203, 187, 175, 84, 100, 82, 15, 11, 23,
				202, 151, 107, 54, 41, 207, 3, 136, 229, 134, 131, 93, 139, 50, 182, 204, 93, 130, 89 }));

		PublicKey publicKey = new RSAPublicKeyImpl(modulus, exponent);
		RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(modulus, privateExponent);

		// Encrypt
		JweEncrypterImpl encrypter = new JweEncrypterImpl(KeyEncryptionAlgorithm.RSA1_5,
				BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, publicKey);
		String encodedJweEncryptedKey = encrypter.generateEncryptedKey(cmk);

		byte[] initVector = Base64Util
				.unsignedToBytes(new int[] { 3, 22, 60, 12, 43, 67, 104, 105, 108, 108, 105, 99, 111, 116, 104, 101 });
		String encodedInitVector = Base64Util.base64urlencode(initVector);
		assertEquals(encodedInitVector, "AxY8DCtDaGlsbGljb3RoZQ");

		String additionalAuthenticatedData = encodedHeader + "." + encodedJweEncryptedKey + "." + encodedInitVector;

		Pair<String, String> cipherTextAndIntegrityValue = encrypter.generateCipherTextAndIntegrityValue(cmk,
				initVector, additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING),
				plainText.getBytes(Util.UTF8_STRING_ENCODING));
		String encodedCipherText = cipherTextAndIntegrityValue.getFirst();
		String encodedAuthenticationTag = cipherTextAndIntegrityValue.getSecond();

		String encodedJwe = encodedHeader + "." + encodedJweEncryptedKey + "." + encodedInitVector + "."
				+ encodedCipherText + "." + encodedAuthenticationTag;
		System.out.println("JWE: " + encodedJwe);

		// Decrypt
		JweDecrypterImpl decrypter = new JweDecrypterImpl(rsaPrivateKey);
		decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.RSA1_5);
		decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
		byte[] encryptionKey = decrypter.decryptEncryptionKey(encodedJweEncryptedKey);
		assertEquals(encryptionKey, cmk);

		String decodedPlainText = decrypter.decryptCipherText(encodedCipherText, encryptionKey, initVector,
				Base64Util.base64urldecode(encodedAuthenticationTag),
				additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING));
		assertEquals(decodedPlainText, plainText);
	}

	@Test
	public void cryptoTest3() throws Exception {
		showTitle("Test: alg = RSA1_5, enc = A256CBC+HS512");

		// {"alg":"RSA1_5","enc":"A256CBC+HS512"}
		String encodedHeader = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMjU2Q0JDK0hTNTEyIn0";
		String plainText = "No matter where you go, there you are.";
		byte[] cmk = new byte[BlockEncryptionAlgorithm.A256CBC_PLUS_HS512.getCmkLength() / 8];
		SecureRandom random = new SecureRandom();
		random.nextBytes(cmk);

		BigInteger modulus = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 177, 119, 33, 13, 164, 30, 108,
				121, 207, 136, 107, 242, 12, 224, 19, 226, 198, 134, 17, 71, 173, 75, 42, 61, 48, 162, 206, 161, 97,
				108, 185, 234, 226, 219, 118, 206, 118, 5, 169, 224, 60, 181, 90, 85, 51, 123, 6, 224, 4, 122, 29, 230,
				151, 12, 244, 127, 121, 25, 4, 85, 220, 144, 215, 110, 130, 17, 68, 228, 129, 138, 7, 130, 231, 40, 212,
				214, 17, 179, 28, 124, 151, 178, 207, 20, 14, 154, 222, 113, 176, 24, 198, 73, 211, 113, 9, 33, 178, 80,
				13, 25, 21, 25, 153, 212, 206, 67, 154, 147, 70, 194, 192, 183, 160, 83, 98, 236, 175, 85, 23, 97, 75,
				199, 177, 73, 145, 50, 253, 206, 32, 179, 254, 236, 190, 82, 73, 67, 129, 253, 252, 220, 108, 136, 138,
				11, 192, 1, 36, 239, 228, 55, 81, 113, 17, 25, 140, 63, 239, 146, 3, 172, 96, 60, 227, 233, 64, 255,
				224, 173, 225, 228, 229, 92, 112, 72, 99, 97, 26, 87, 187, 123, 46, 50, 90, 202, 117, 73, 10, 153, 47,
				224, 178, 163, 77, 48, 46, 154, 33, 148, 34, 228, 33, 172, 216, 89, 46, 225, 127, 68, 146, 234, 30, 147,
				54, 146, 5, 133, 45, 78, 254, 85, 55, 75, 213, 86, 194, 218, 215, 163, 189, 194, 54, 6, 83, 36, 18, 153,
				53, 7, 48, 89, 35, 66, 144, 7, 65, 154, 13, 97, 75, 55, 230, 132, 3, 13, 239, 71 }));

		BigInteger exponent = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 1, 0, 1 }));

		BigInteger privateExponent = new BigInteger(1, Base64Util.unsignedToBytes(new int[] { 84, 80, 150, 58, 165, 235,
				242, 123, 217, 55, 38, 154, 36, 181, 221, 156, 211, 215, 100, 164, 90, 88, 40, 228, 83, 148, 54, 122, 4,
				16, 165, 48, 76, 194, 26, 107, 51, 53, 179, 165, 31, 18, 198, 173, 78, 61, 56, 97, 252, 158, 140, 80,
				63, 25, 223, 156, 36, 203, 214, 252, 120, 67, 180, 167, 3, 82, 243, 25, 97, 214, 83, 133, 69, 16, 104,
				54, 160, 200, 41, 83, 164, 187, 70, 153, 111, 234, 242, 158, 175, 28, 198, 48, 211, 45, 148, 58, 23, 62,
				227, 74, 52, 117, 42, 90, 41, 249, 130, 154, 80, 119, 61, 26, 193, 40, 125, 10, 152, 174, 227, 225, 205,
				32, 62, 66, 6, 163, 100, 99, 219, 19, 253, 25, 105, 80, 201, 29, 252, 157, 237, 69, 1, 80, 171, 167, 20,
				196, 156, 109, 249, 88, 0, 3, 152, 38, 165, 72, 87, 6, 152, 71, 156, 214, 16, 71, 30, 82, 51, 103, 76,
				218, 63, 9, 84, 163, 249, 91, 215, 44, 238, 85, 101, 240, 148, 1, 82, 224, 91, 135, 105, 127, 84, 171,
				181, 152, 210, 183, 126, 24, 46, 196, 90, 173, 38, 245, 219, 186, 222, 27, 240, 212, 194, 15, 66, 135,
				226, 178, 190, 52, 245, 74, 65, 224, 81, 100, 85, 25, 204, 165, 203, 187, 175, 84, 100, 82, 15, 11, 23,
				202, 151, 107, 54, 41, 207, 3, 136, 229, 134, 131, 93, 139, 50, 182, 204, 93, 130, 89 }));

		PublicKey publicKey = new RSAPublicKeyImpl(modulus, exponent);
		RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(modulus, privateExponent);

		// Encrypt
		JweEncrypterImpl encrypter = new JweEncrypterImpl(KeyEncryptionAlgorithm.RSA1_5,
				BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, publicKey);
		String encodedJweEncryptedKey = encrypter.generateEncryptedKey(cmk);

		byte[] initVector = Base64Util
				.unsignedToBytes(new int[] { 3, 22, 60, 12, 43, 67, 104, 105, 108, 108, 105, 99, 111, 116, 104, 101 });
		String encodedInitVector = Base64Util.base64urlencode(initVector);
		assertEquals(encodedInitVector, "AxY8DCtDaGlsbGljb3RoZQ");

		String additionalAuthenticatedData = encodedHeader + "." + encodedJweEncryptedKey + "." + encodedInitVector;

		Pair<String, String> cipherTextAndIntegrityValue = encrypter.generateCipherTextAndIntegrityValue(cmk,
				initVector, additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING),
				plainText.getBytes(Util.UTF8_STRING_ENCODING));
		String encodedCipherText = cipherTextAndIntegrityValue.getFirst();
		String encodedAuthenticationTag = cipherTextAndIntegrityValue.getSecond();

		String encodedJwe = encodedHeader + "." + encodedJweEncryptedKey + "." + encodedInitVector + "."
				+ encodedCipherText + "." + encodedAuthenticationTag;
		System.out.println("JWE: " + encodedJwe);

		// Decrypt
		JweDecrypterImpl decrypter = new JweDecrypterImpl(rsaPrivateKey);
		decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.RSA1_5);
		decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
		byte[] encryptionKey = decrypter.decryptEncryptionKey(encodedJweEncryptedKey);
		assertEquals(encryptionKey, cmk);

		String decodedPlainText = decrypter.decryptCipherText(encodedCipherText, encryptionKey, initVector,
				Base64Util.base64urldecode(encodedAuthenticationTag),
				additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING));
		assertEquals(decodedPlainText, plainText);
	}

	@Test
	public void cryptoTest4() throws Exception {
		showTitle("Test: alg = A128KW, enc = A128GCM");

		// {"alg":"A128KW","enc":"A128GCM"}
		String encodedHeader = "eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0";
		String plainText = "The true sign of intelligence is not knowledge but imagination.";
		byte[] cmk = Base64Util.unsignedToBytes(
				new int[] { 64, 154, 239, 170, 64, 40, 195, 99, 19, 84, 192, 142, 192, 238, 207, 217 });

		byte[] sharedSymmetricKey = Base64Util.unsignedToBytes(
				new int[] { 25, 172, 32, 130, 225, 114, 26, 181, 138, 106, 254, 192, 95, 133, 74, 82 });

		// Encrypt
		JweEncrypterImpl encrypter = new JweEncrypterImpl(KeyEncryptionAlgorithm.A128KW,
				BlockEncryptionAlgorithm.A128GCM, sharedSymmetricKey);
		String encodedEncryptedKey = encrypter.generateEncryptedKey(cmk);
		assertEquals(encodedEncryptedKey, "pP_7AUDIQcgixVGPK9PwJr-htXV3RCxQ");

		byte[] initVector = Base64Util
				.unsignedToBytes(new int[] { 253, 220, 80, 25, 166, 152, 178, 168, 97, 99, 67, 89 });
		String encodedInitVector = Base64Util.base64urlencode(initVector);
		assertEquals(encodedInitVector, "_dxQGaaYsqhhY0NZ");

		String additionalAuthenticatedData = encodedHeader + "." + encodedEncryptedKey + "." + encodedInitVector;

		Pair<String, String> cipherTextAndIntegrityValue = encrypter.generateCipherTextAndIntegrityValue(cmk,
				initVector, additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING),
				plainText.getBytes(Util.UTF8_STRING_ENCODING));
		String encodedCipherText = cipherTextAndIntegrityValue.getFirst();
		String encodedAuthenticationTag = cipherTextAndIntegrityValue.getSecond();

		assertEquals(encodedCipherText,
				"4wxZhLkQ-F2RVzWCX3M-aIpgbUd806VnymMVwQTiVOX-apDxJ1aUhKBoWOjkbVUHVlCGaqYYXMfSvJm72kXj");
		assertEquals(encodedAuthenticationTag, "miNQayWUUQZnBDzOq6VxQw");

		String encodedJwe = encodedHeader + "." + encodedEncryptedKey + "." + encodedInitVector + "."
				+ encodedCipherText + "." + encodedAuthenticationTag;
		System.out.println("JWE: " + encodedJwe);

		// Decrypt
		JweDecrypterImpl decrypter = new JweDecrypterImpl(sharedSymmetricKey);
		decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.A128KW);
		decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A128GCM);
		byte[] encryptionKey = decrypter.decryptEncryptionKey(encodedEncryptedKey);
		assertEquals(encryptionKey, cmk);

		String decodedPlainText = decrypter.decryptCipherText(encodedCipherText, encryptionKey, initVector,
				Base64Util.base64urldecode(encodedAuthenticationTag),
				additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING));
		assertEquals(decodedPlainText, plainText);
	}

	@Test
	public void cryptoTest5() throws Exception {
		showTitle("Test: alg = A256KW, enc = A256GCM");

		// {"alg":"A256KW","enc":"A256GCM"}
		String encodedHeader = "eyJhbGciOiJBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIn0";
		String plainText = "The true sign of intelligence is not knowledge but imagination.";
		byte[] cmk = new byte[BlockEncryptionAlgorithm.A256GCM.getCmkLength() / 8];
		SecureRandom random = new SecureRandom();
		random.nextBytes(cmk);

		byte[] sharedSymmetricKey = Base64Util.unsignedToBytes(
				new int[] { 25, 172, 32, 130, 225, 114, 26, 181, 138, 106, 254, 192, 95, 133, 74, 82 });

		// Encrypt
		JweEncrypterImpl encrypter = new JweEncrypterImpl(KeyEncryptionAlgorithm.A256KW,
				BlockEncryptionAlgorithm.A256GCM, sharedSymmetricKey);
		String encodedEncryptedKey = encrypter.generateEncryptedKey(cmk);

		byte[] initVector = Base64Util
				.unsignedToBytes(new int[] { 253, 220, 80, 25, 166, 152, 178, 168, 97, 99, 67, 89 });
		String encodedInitVector = Base64Util.base64urlencode(initVector);
		assertEquals(encodedInitVector, "_dxQGaaYsqhhY0NZ");

		String additionalAuthenticatedData = encodedHeader + "." + encodedEncryptedKey + "." + encodedInitVector;

		Pair<String, String> cipherTextAndIntegrityValue = encrypter.generateCipherTextAndIntegrityValue(cmk,
				initVector, additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING),
				plainText.getBytes(Util.UTF8_STRING_ENCODING));
		String encodedCipherText = cipherTextAndIntegrityValue.getFirst();
		String encodedAuthenticationTag = cipherTextAndIntegrityValue.getSecond();

		String encodedJwe = encodedHeader + "." + encodedEncryptedKey + "." + encodedInitVector + "."
				+ encodedCipherText + "." + encodedAuthenticationTag;
		System.out.println("JWE: " + encodedJwe);

		// Decrypt
		JweDecrypterImpl decrypter = new JweDecrypterImpl(sharedSymmetricKey);
		decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.A256KW);
		decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A256GCM);
		byte[] encryptionKey = decrypter.decryptEncryptionKey(encodedEncryptedKey);
		assertEquals(encryptionKey, cmk);

		String decodedPlainText = decrypter.decryptCipherText(encodedCipherText, encryptionKey, initVector,
				Base64Util.base64urldecode(encodedAuthenticationTag),
				additionalAuthenticatedData.getBytes(Util.UTF8_STRING_ENCODING));
		assertEquals(decodedPlainText, plainText);
	}
	*/
}