/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.fido2.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import io.jans.as.model.util.SecurityProviderUtility;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 0.1, 17/02/2023
 */
@ExtendWith(WeldJunit5AutoExtension.class)
@AddPackages(io.jans.service.util.Resources.class)
public class KeySignatureVerifierTest {

	@Inject
	Base64Service base64Service;

	@BeforeAll
    public static void beforeAll() {
		System.out.println("!!!!!!!");
		SecurityProviderUtility.installBCProvider();
    }

	/*
	 * openssl ecparam -name secp256r1 -genkey -noout -out private.key
	 * openssl ec -in private.key -pubout -out public.pem
	 * echo -n "test" > data.txt
	 * 
	 * openssl dgst -sha256 -sign private.key data.txt | openssl enc -base64 > signature.txt
	 */
	//@Test
	public void testSHA256withECDSASignature() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, SignatureException {
		System.out.println(new File(".").getAbsolutePath());
		String key = FileUtils.readFileToString(new File("./target/test-classes/keys/secp256r1/public.pem"), StandardCharsets.UTF_8);
	    String publicKeyPEM = key.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll(System.lineSeparator(), "").replace("-----END PUBLIC KEY-----", "");

	    KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
	    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(base64Service.decode(publicKeyPEM));
	    PublicKey publicKey = keyFactory.generatePublic(keySpec);
	    
		byte[] signature = base64Service.decode(FileUtils.readFileToString(new File("./target/test-classes/keys/secp256r1/signature.txt"),
				StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), ""));
		byte[] signedBytes = FileUtils.readFileToString(new File("./target/test-classes/keys/secp256r1/data.txt"), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);

		Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA", "BC");
		ecdsaSignature.initVerify(publicKey);
		ecdsaSignature.update(signedBytes);

		boolean isValid = ecdsaSignature.verify(signature);
		assertTrue(isValid);
	}

}
