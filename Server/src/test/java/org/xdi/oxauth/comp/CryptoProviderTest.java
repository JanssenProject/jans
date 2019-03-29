/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.CryptoProviderFactory;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.Algorithm;
import org.testng.annotations.Test;
import org.xdi.oxauth.ConfigurableTest;
import org.xdi.oxauth.model.config.ConfigurationFactory;

import javax.inject.Inject;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.gluu.oxeleven.model.GenerateKeyResponseParam.KEY_ID;
import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class CryptoProviderTest extends ConfigurableTest {

	@Inject
	private ConfigurationFactory configurationFactory;

	private final String SIGNING_INPUT = "Signing Input";
	private final String SHARED_SECRET = "secret";

	private static AbstractCryptoProvider cryptoProvider;

	private static Long expirationTime;
	private static String hs256Signature;
	private static String hs384Signature;
	private static String hs512Signature;
	private static String rs256Key;
	private static String rs256Signature;
	private static String rs384Key;
	private static String rs384Signature;
	private static String rs512Key;
	private static String rs512Signature;
	private static String es256Key;
	private static String es256Signature;
	private static String es384Key;
	private static String es384Signature;
	private static String es512Key;
	private static String es512Signature;

	@Test
	public void configuration() {
		try {
			AppConfiguration appConfiguration = configurationFactory.getAppConfiguration();
			assertNotNull(appConfiguration);

			cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);
			assertNotNull(cryptoProvider);

			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.add(GregorianCalendar.MINUTE, 5);
			expirationTime = calendar.getTimeInMillis();
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testSignHS256() {
		try {
			hs256Signature = cryptoProvider.sign(SIGNING_INPUT, null, SHARED_SECRET, SignatureAlgorithm.HS256);
			assertNotNull(hs256Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignHS256"})
	public void testVerifyHS256() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, hs256Signature, null, null,
					SHARED_SECRET, SignatureAlgorithm.HS256);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testSignHS384() {
		try {
			hs384Signature = cryptoProvider.sign(SIGNING_INPUT, null, SHARED_SECRET, SignatureAlgorithm.HS384);
			assertNotNull(hs384Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignHS384"})
	public void testVerifyHS384() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, hs384Signature, null, null,
					SHARED_SECRET, SignatureAlgorithm.HS384);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testSignHS512() {
		try {
			hs512Signature = cryptoProvider.sign(SIGNING_INPUT, null, SHARED_SECRET, SignatureAlgorithm.HS512);
			assertNotNull(hs512Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignHS512"})
	public void testVerifyHS512() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, hs512Signature, null, null,
					SHARED_SECRET, SignatureAlgorithm.HS512);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testGenerateKeyRS256() {
		try {
			JSONObject response = cryptoProvider.generateKey(Algorithm.RS256, expirationTime);
			rs256Key = response.optString(KEY_ID);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testGenerateKeyRS256"})
	public void testSignRS256() {
		try {
			rs256Signature = cryptoProvider.sign(SIGNING_INPUT, rs256Key, null, SignatureAlgorithm.RS256);
			assertNotNull(rs256Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignRS256"})
	public void testVerifyRS256() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, rs256Signature, rs256Key, null,
					null, SignatureAlgorithm.RS256);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testVerifyRS256"})
	public void testDeleteKeyRS256() {
		try {
			cryptoProvider.deleteKey(rs256Key);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testGenerateKeyRS384() {
		try {
			JSONObject response = cryptoProvider.generateKey(Algorithm.RS384, expirationTime);
			rs384Key = response.optString(KEY_ID);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testGenerateKeyRS384"})
	public void testSignRS384() {
		try {
			rs384Signature = cryptoProvider.sign(SIGNING_INPUT, rs384Key, null, SignatureAlgorithm.RS384);
			assertNotNull(rs384Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignRS384"})
	public void testVerifyRS384() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, rs384Signature, rs384Key, null,
					null, SignatureAlgorithm.RS384);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testVerifyRS384"})
	public void testDeleteKeyRS384() {
		try {
			cryptoProvider.deleteKey(rs384Key);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testGenerateKeyRS512() {
		try {
			JSONObject response = cryptoProvider.generateKey(Algorithm.RS512, expirationTime);
			rs512Key = response.optString(KEY_ID);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testGenerateKeyRS512"})
	public void testSignRS512() {
		try {
			rs512Signature = cryptoProvider.sign(SIGNING_INPUT, rs512Key, null, SignatureAlgorithm.RS512);
			assertNotNull(rs512Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignRS512"})
	public void testVerifyRS512() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, rs512Signature, rs512Key, null,
					null, SignatureAlgorithm.RS512);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testVerifyRS512"})
	public void testDeleteKeyRS512() {
		try {
			cryptoProvider.deleteKey(rs512Key);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testGenerateKeyES256() {
		try {
			JSONObject response = cryptoProvider.generateKey(Algorithm.ES256, expirationTime);
			es256Key = response.optString(KEY_ID);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testGenerateKeyES256"})
	public void testSignES256() {
		try {
			es256Signature = cryptoProvider.sign(SIGNING_INPUT, es256Key, null, SignatureAlgorithm.ES256);
			assertNotNull(es256Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignES256"})
	public void testVerifyES256() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, es256Signature, es256Key, null,
					null, SignatureAlgorithm.ES256);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testVerifyES256"})
	public void testDeleteKeyES256() {
		try {
			cryptoProvider.deleteKey(es256Key);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testGenerateKeyES384() {
		try {
			JSONObject response = cryptoProvider.generateKey(Algorithm.ES384, expirationTime);
			es384Key = response.optString(KEY_ID);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testGenerateKeyES384"})
	public void testSignES384() {
		try {
			es384Signature = cryptoProvider.sign(SIGNING_INPUT, es384Key, null, SignatureAlgorithm.ES384);
			assertNotNull(es384Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignES384"})
	public void testVerifyES384() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, es384Signature, es384Key, null,
					null, SignatureAlgorithm.ES384);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testVerifyES384"})
	public void testDeleteKeyES384() {
		try {
			cryptoProvider.deleteKey(es384Key);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"configuration"})
	public void testGenerateKeyES512() {
		try {
			JSONObject response = cryptoProvider.generateKey(Algorithm.ES512, expirationTime);
			es512Key = response.optString(KEY_ID);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testGenerateKeyES512"})
	public void testSignES512() {
		try {
			es512Signature = cryptoProvider.sign(SIGNING_INPUT, es512Key, null, SignatureAlgorithm.ES512);
			assertNotNull(es512Signature);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testSignES512"})
	public void testVerifyES512() {
		try {
			boolean signatureVerified = cryptoProvider.verifySignature(SIGNING_INPUT, es512Signature, es512Key, null,
					null, SignatureAlgorithm.ES512);
			assertTrue(signatureVerified);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

	@Test(dependsOnMethods = {"testVerifyES512"})
	public void testDeleteKeyES512() {
		try {
			cryptoProvider.deleteKey(es512Key);
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}
}