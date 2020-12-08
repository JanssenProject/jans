/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import io.jans.eleven.model.JwksRequestParam;
import io.jans.eleven.model.KeyRequestParam;
import io.jans.eleven.model.SignatureAlgorithm;
import org.apache.http.HttpStatus;
import io.jans.eleven.client.BaseClient;
import io.jans.eleven.client.ClientUtils;
import io.jans.eleven.client.DeleteKeyClient;
import io.jans.eleven.client.DeleteKeyRequest;
import io.jans.eleven.client.DeleteKeyResponse;
import io.jans.eleven.client.GenerateKeyClient;
import io.jans.eleven.client.GenerateKeyRequest;
import io.jans.eleven.client.GenerateKeyResponse;
import io.jans.eleven.client.SignClient;
import io.jans.eleven.client.SignRequest;
import io.jans.eleven.client.SignResponse;
import io.jans.eleven.client.VerifySignatureClient;
import io.jans.eleven.client.VerifySignatureRequest;
import io.jans.eleven.client.VerifySignatureResponse;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public class PKCS11RestServiceTest {

    private Long expirationTime;
    private String rs256Alias;
    private String rs384Alias;
    private String rs512Alias;
    private String es256Alias;
    private String es384Alias;
    private String es512Alias;
    private String noneSignature;
    private String hs256Signature;
    private String hs384Signature;
    private String hs512Signature;
    private String rs256Signature;
    private String rs384Signature;
    private String rs512Signature;
    private String es256Signature;
    private String es384Signature;
    private String es512Signature;

    @BeforeSuite
    public void init() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(GregorianCalendar.MINUTE, 5);
        expirationTime = calendar.getTimeInMillis();
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyUnauthorized1(final String generateKeyEndpoint) {
        try {
            showTitle("testGenerateKeyUnauthorized1");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken("INVALID_ACCESS_TOKEN");
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyUnauthorized2(final String generateKeyEndpoint) {
        try {
            showTitle("testGenerateKeyUnauthorized2");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(null);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testSignatureUnauthorized1(final String signEndpoint, final String signingInput, final String sharedSecret) {
        try {
            showTitle("testSignatureUnauthorized1");

            SignRequest request = new SignRequest();
            request.setAccessToken("INVALID_ACCESS_TOKEN");
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testSignatureUnauthorized2(final String signEndpoint, final String signingInput, final String sharedSecret) {
        try {
            showTitle("testSignatureUnauthorized2");

            SignRequest request = new SignRequest();
            request.setAccessToken(null);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testVerifySignatureUnauthorized1(final String verifySignatureEndpoint, final String signingInput, final String sharedSecret) {
        try {
            showTitle("testVerifySignatureUnauthorized1");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken("INVALID_ACCESS_TOKEN");
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs256Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testVerifySignatureUnauthorized2(final String verifySignatureEndpoint, final String signingInput, final String sharedSecret) {
        try {
            showTitle("testVerifySignatureUnauthorized2");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(null);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs256Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test
    public void testDeleteKeyUnauthorized1(final String deleteKeyEndpoint) {
        try {
            showTitle("testDeleteKeyUnauthorized1");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken("INVALID_ACCESS_TOKEN");
            request.setAlias(rs256Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test
    public void testDeleteKeyUnauthorized2(final String deleteKeyEndpoint) {
        try {
            showTitle("testDeleteKeyUnauthorized2");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(null);
            request.setAlias(rs256Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_UNAUTHORIZED);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyRS256(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyRS256");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            rs256Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyRS384(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyRS384");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            rs384Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyRS512(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyRS512");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            rs512Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyES256(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyES256");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            es256Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyES384(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyES384");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            es384Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyES512(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyES512");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            es512Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail1(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail1");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(null);
            request.setExpirationTime(null);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail2(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail2");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(null);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail3(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail3");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
            request.setExpirationTime(null);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail4(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail4");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.NONE);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail5(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail5");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.HS256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail6(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail6");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.HS384);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint", "testModeToken"})
    @Test
    public void testGenerateKeyFail7(final String generateKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testGenerateKeyFail7");

            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setAccessToken(testModeToken);
            request.setSignatureAlgorithm(SignatureAlgorithm.HS512);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test
    public void testSignatureNone(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureNone");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.NONE);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertEquals("", response.getSignature());
            noneSignature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret", "testModeToken"})
    @Test
    public void testSignatureHS256(final String signEndpoint, final String signingInput, final String sharedSecret,
                                   final String testModeToken) {
        try {
            showTitle("testSignatureHS256");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            hs256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret", "testModeToken"})
    @Test
    public void testSignatureHS384(final String signEndpoint, final String signingInput, final String sharedSecret,
                                   final String testModeToken) {
        try {
            showTitle("testSignatureHS384");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS384);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            hs384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret", "testModeToken"})
    @Test
    public void testSignatureHS512(final String signEndpoint, final String signingInput, final String sharedSecret,
                                   final String testModeToken) {
        try {
            showTitle("testSignatureHS512");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS512);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            hs512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureRS256(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureRS256");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS384")
    public void testSignatureRS384(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureRS384");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs384Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS384);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS512")
    public void testSignatureRS512(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureRS512");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs512Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS512);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyES256")
    public void testSignatureES256(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureES256");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(es256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyES384")
    public void testSignatureES384(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureES384");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(es384Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES384);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyES512")
    public void testSignatureES512(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureES512");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(es512Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES512);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "testModeToken"})
    @Test
    public void testSignatureFail1(final String signEndpoint, final String testModeToken) {
        try {
            showTitle("testSignatureFail1");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(null);
            request.getSignRequestParam().setAlias(null);
            request.getSignRequestParam().setSignatureAlgorithm(null);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail2(final String signEndpoint, final String testModeToken) {
        try {
            showTitle("testSignatureFail2");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(null);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail3(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureFail3");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(null);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test
    public void testSignatureFail4(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureFail4");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias("INVALID_ALIAS");
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail5(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureFail5");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail6(final String signEndpoint, final String signingInput, final String testModeToken) {
        try {
            showTitle("testSignatureFail6");

            SignRequest request = new SignRequest();
            request.setAccessToken(testModeToken);
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(null);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureNone")
    public void testVerifySignatureNone(final String verifySignatureEndpoint, final String signingInput,
                                        final String testModeToken) {
        try {
            showTitle("testVerifySignatureNone");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(noneSignature);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.NONE);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureHS256")
    public void testVerifySignatureHS256(final String verifySignatureEndpoint, final String signingInput,
                                         final String sharedSecret, final String testModeToken) {
        try {
            showTitle("testVerifySignatureHS256");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs256Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureHS384")
    public void testVerifySignatureHS384(final String verifySignatureEndpoint, final String signingInput,
                                         final String sharedSecret, final String testModeToken) {
        try {
            showTitle("testVerifySignatureHS384");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs384Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureHS512")
    public void testVerifySignatureHS512(final String verifySignatureEndpoint, final String signingInput,
                                         final String sharedSecret, final String testModeToken) {
        try {
            showTitle("testVerifySignatureHS512");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs512Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureRS256(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureRS256");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureRS256Jwks(final String verifySignatureEndpoint, final String testModeToken) {
        showTitle("testVerifySignatureRS256Jwks");

        String signingInput = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlJTMjU2U0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCEzN0JBLkExRjEiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCEzN0JBLkExRjEiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6Ijc0NWY0N2RmLTY3ZDQtNDBlOC05MzhlLTVlMmI5OWQ5ZTQ3YSIsImV4cCI6MTQ2MTAzMDE5MSwiaWF0IjoxNDYxMDI5ODkxfQ";
        String signature = "RB8KEbzMTovJLGBzxbaxzLvZxj0CjAun1LG1KMuw9t9LBNzA9kxt_QT9qm_vr_SpCFuFhIy6ZeDx4lVPGks6JbWOYxmsCUcxe8l_tkCxOb6fwm3GTttDhHsk1JKPwDVjzXWAyW8i5Wiv39JD57K1SOs3xIOWIp7Uu7lR7HFw52ybT35enxiaGj1H3ROX5dd26GE35McTrEBxPLgAj_yEzAADBqI1nOmDvpzSpo3pkSoxaW8UkncIIdcG8WkPru-exN1nWqnsqA5rX3XxwlWNElq6O9kLOZQKKHbCF0EyZwnave3EdWp56XaZ9V5Y20_NL-aaR7DedZ5xPAyzLFCW2A";
        String alias = "RS256SIG";
        JwksRequestParam jwksRequestParam = new JwksRequestParam();
        KeyRequestParam keyRequestParam = new KeyRequestParam("RSA", "sig", "RS256", alias);
        keyRequestParam.setN("AJpGcIVu7fmQJLHXeAClhXaJD7SvuABjYiPcT9IbKFWGWj51GgD-CxtyrQGXT0ctGEEsXOzMZM40q-V7GR-5qkJ_OalVTTc_EeKAHao45bZPsPHLxvusNfrfpyhc6JjF2TQhoOqxbgMgQ9L6W9q9fSjgzx-tPlD0d3X0GZOEQ_NYGstZWRRBwHgsxA2IRYtwSH-v76yPpxF9poLIWdnBKtKfSr6UY7p1BrLmMm0DdMhjQLn6j4S_eB-p2WyBwObvsLqO6FdClpZFtGr82Km2uinpHvZ6KJ_MUEW1sijPPI3rIGbaUbLtQJwX5GVynAP5qU2qRVkcsrKt-GeNoz6QNLM");
        keyRequestParam.setE("AQAB");
        jwksRequestParam.setKeyRequestParams(Arrays.asList(
                keyRequestParam
        ));

        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS384")
    public void testVerifySignatureRS384(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureRS384");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs384Signature);
            request.getVerifySignatureRequestParam().setAlias(rs384Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureRS384Jwks(final String verifySignatureEndpoint, final String testModeToken) {
        showTitle("testVerifySignatureRS384Jwks");

        String signingInput = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzM4NCIsImtpZCI6IlJTMzg0U0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFBRjk1LkRERDYiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFBRjk1LkRERDYiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6IjVmNjU2YTVlLTkzMWMtNDVkYi1hNDIxLTRiYmY5YzA0NjMzNiIsImV4cCI6MTQ2MTAzMjE5MiwiaWF0IjoxNDYxMDMxODkyfQ";
        String signature = "KDa4GI5nk6CB_g7_gn9N1k0c7At_ZJB0e0_dAChiBPQ1pgaqcWhyotgMhuStIe05WtOiF3JoMIHwJUqns81LTE4LxAWrmxIqoevvBL4lra7Cy33GZOQeYQTSECO3SurQ6MMWLiOKF5_bMbhy2vpxkNtdsdrF_0DGq6MI2Sk_xlgIGLdUpVSJDZ2E_hXvh3QD1v-ryi1NUIQvsyQorfdhu0SG0mB5QgeCWy6mpYQhoqaFK6WLzL4Uf6aghP-KqC33Y6zHySalcXxe3tNvdtaXjsWonUp81mksBwAFJsXyILaxH8IdMyZtJ0lZgHf3Mq_dDda-h2sCl-Wf4mLN7kyS9A";
        String alias = "RS384SIG";
        JwksRequestParam jwksRequestParam = new JwksRequestParam();
        KeyRequestParam keyRequestParam = new KeyRequestParam("RSA", "sig", "RS384", alias);
        keyRequestParam.setN("AJ125IzZ0TRSSoVas3jwMWuckyMujoGUUeDd8rLjTSCLlgUb3RiT9MbKfWdeCByme5MZ21lvMu6OmMFn8iDb5erLSBJ8bZFq6ruGIVzU8NI833IahlIO9m6JIR4L_go8Szu-1MYPGUjOKDsxc-Fp3fR-Kb0HFAEEs44t9vL9yMKjNeQeAp7Fo2AukDNEZqvEObP7XWLdJFA-TuAXE1f7o49lMr0y4Tqy2XeDKwfklO0bAnbSryZubRg2E7gjiwaiSYVIFphotLlpCd3N4MU46JjHA2dv1GtIe8749HinwhK1stes3PbZb9Gwm2LyK89iRJ35bCmDLnkwP0rTwTZ2Ul8");
        keyRequestParam.setE("AQAB");
        jwksRequestParam.setKeyRequestParams(Arrays.asList(
                keyRequestParam
        ));

        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS512")
    public void testVerifySignatureRS512(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureRS512");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs512Signature);
            request.getVerifySignatureRequestParam().setAlias(rs512Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureRS512Jwks(final String verifySignatureEndpoint, final String testModeToken) {
        showTitle("testVerifySignatureRS512Jwks");

        String signingInput = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiIsImtpZCI6IlJTNTEyU0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFDRUY2LkQxNzQiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFDRUY2LkQxNzQiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6ImIyNGQzMDYzLTc0MmQtNGU3NS1hMWQyLTViOTQ2NzQ3YzMyMSIsImV4cCI6MTQ2MTAzMjMzNSwiaWF0IjoxNDYxMDMyMDM1fQ";
        String signature = "Uummz_uBqVyf_9d5obKpIs5p7MTPwtxo9_knsv0eJ_2ObuOBsRUO2LcV7pDoQ0-XZ6GvBYBSsT4-IKic_2HW29qqvKo0c833xPnduyodUtTp9wCDOEY3_5CBOaj6PA-39aAcai1ybrQ1JNqe90XcIgLYVsFmTI-iX4p6bpNrY2oXKuYhQEJKf1O4-8w4xVi5GmsOiQVJAJdYVvUWJwTJhzi8jX1a6iQUC4TEPdOSbZ9ctxvumf-KoCbUwDkf6tKePDGkvJqHQkpTFtSWuL5QaJI79fhFV4TDDBo-Mpc_B9mqWYzXVG4zYpWR7wU8AeTJMkyPjAlNXF5RLsm8jGqfYw";
        String alias = "RS512SIG";
        JwksRequestParam jwksRequestParam = new JwksRequestParam();
        KeyRequestParam keyRequestParam = new KeyRequestParam("RSA", "sig", "RS512", alias);
        keyRequestParam.setN("AKuc75KyKNwteumhyN5Kxa4ipQZrE_ouULtMZmCYI3Y32oCv3wWkgmrprBo-yCK292wfn77dNdZ9h5OoY-6sDVG-OKi9uwXpFcopyqIdsYOrw-4FKHxpr_7b--cH6HRmGlSFKVJpwfvIjD9Mu8S9bhNgnXfbKoYLcANU7Vjtacr3MvX-U406eRXLI9lZNr6ViQxSJw3A7yYMo2XYMYhO-FHGOYeV815q7fJFUMoCUMNSWlCx-pUCVGg0PuCKlOhUGIoLqvuFqUnBNd0hoAJCtmqya4_e3DLNzOgr2HOEbX7kQEjpi0XdyQ0fbFTAYO9TpXT2gldnmOElZ4UE2lX8J6M");
        keyRequestParam.setE("AQAB");
        jwksRequestParam.setKeyRequestParams(Arrays.asList(
                keyRequestParam
        ));

        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureES256")
    public void testVerifySignatureES256(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureES256");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(es256Signature);
            request.getVerifySignatureRequestParam().setAlias(es256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureES256Jwks(final String verifySignatureEndpoint, final String testModeToken) {
        showTitle("testVerifySignatureES256Jwks");

        String signingInput = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IkVTMjU2U0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCE3OUIzLjY3MzYiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCE3OUIzLjY3MzYiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6IjQ0ZjU0NmU0LWRmMmMtNDE5Ny1iNTNjLTIzNzhmY2YwYmRiZSIsImV4cCI6MTQ2MTAzMjgzMiwiaWF0IjoxNDYxMDMyNTMyfQ";
        String signature = "MEQCIGmPSoCExpDu2jPkxttRZ0hjKId9SQM1pP3PLd4CXmt9AiB57tUzvBILyBvHqf3bHVMi0Fsy8M-v-ERib2KVdWJLtg";
        String alias = "ES256SIG";
        JwksRequestParam jwksRequestParam = new JwksRequestParam();
        KeyRequestParam keyRequestParam = new KeyRequestParam("EC", "sig", "ES256", alias);
        keyRequestParam.setCrv("P-256");
        keyRequestParam.setX("QDpwgxzGm0XdD-3Rgk62wiUnayJDS5iV7nLBwNEX4SI");
        keyRequestParam.setY("AJ3IvktOcoICgdFPAvBM44glxcqoHzqyEmj60eATGf5e");
        jwksRequestParam.setKeyRequestParams(Arrays.asList(
                keyRequestParam
        ));

        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureES384")
    public void testVerifySignatureES384(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureES384");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(es384Signature);
            request.getVerifySignatureRequestParam().setAlias(es384Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureES384Jwks(final String verifySignatureEndpoint, final String testModeToken) {
        showTitle("testVerifySignatureES384Jwks");

        String signingInput = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCIsImtpZCI6IkVTMzg0U0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFCOUE2LkFBNUIiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFCOUE2LkFBNUIiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6IjgyMmEzMjQwLTI0NjEtNGEwYS1hNDZlLTIwNTEwMDliZjI3NCIsImV4cCI6MTQ2MTAzMzQ2MiwiaWF0IjoxNDYxMDMzMTYyfQ";
        String signature = "MGUCMQCOELLrt3FSaEamp37D6S3XECWqHy-Iriry_tM5ZVLZ-z6aruZTVHJnNnFfyLXUKaYCMFMrKuXCbXjTZKqFv7v3UsJdY4F1S5IVGtIadoxxm-Ayw7rMzu7vTiaQYiRZZbSurw";
        String alias = "ES384SIG";
        JwksRequestParam jwksRequestParam = new JwksRequestParam();
        KeyRequestParam keyRequestParam = new KeyRequestParam("EC", "sig", "ES384", alias);
        keyRequestParam.setCrv("P-384");
        keyRequestParam.setX("AOexLbIW0h2TE7jesYvuuU1oKXSyORC8uR-bHbGw0i5o85jxw7yh1c2H6Bj9o9KCow");
        keyRequestParam.setY("AKGBJpjnucEd-amtI3qQfBreB4rBDilnEdfO6eodLkJbxbRypkW-i4AeCRFe4q8YbA");
        jwksRequestParam.setKeyRequestParams(Arrays.asList(
                keyRequestParam
        ));

        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureES512")
    public void testVerifySignatureES512(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureES512");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(es512Signature);
            request.getVerifySignatureRequestParam().setAlias(es512Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureES512Jwks(final String verifySignatureEndpoint, final String testModeToken) {
        showTitle("testVerifySignatureES512Jwks");

        String signingInput = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzUxMiIsImtpZCI6IkVTNTEyU0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFCQzRFLjBFQUUiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCFCQzRFLjBFQUUiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6ImI0ZjUzMmIyLWZmNzgtNGIxNS04Y2NkLWYwMGIxMWE1ODNlNyIsImV4cCI6MTQ2MTAzMzUyOCwiaWF0IjoxNDYxMDMzMjI4fQ";
        String signature = "MIGHAkFhmQZew3s2L23BpwhqTPxatHkEdqprogXBPCy1qQ5w6n288UrDm_t283nkFI9FklPqoHGr6ZT9hCOjET6mB732kwJCAOTBMwyDmZx9zuRXORH7ZG86Bj488CY75FkWcKfk8AuJyYFQbrJhPTDNmEpyx7f_AKjUlEk9eQcTQxMQX8VFwOi9";
        String alias = "ES512SIG";
        JwksRequestParam jwksRequestParam = new JwksRequestParam();
        KeyRequestParam keyRequestParam = new KeyRequestParam("EC", "sig", "ES512", alias);
        keyRequestParam.setCrv("P-521");
        keyRequestParam.setX("AfQvST7JCl7OJ9o9MYFuv2lPrx-QU0PSMGXpiBe5GPLUtaQXiBa04HG8kGc345ao7P3Cb9yI7lFY2X8nYhz2u26e");
        keyRequestParam.setY("AVo7Ss0s9MFUtJsuHarBgmH_HYom7_1zAJrTlTUIxCsq0b71YYOocYquCaLIlNq4O_Hz7D9eHjby7V5W253Gy2cQ");
        jwksRequestParam.setKeyRequestParams(Arrays.asList(
                keyRequestParam
        ));

        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test
    public void testVerifySignatureFail1(final String verifySignatureEndpoint, final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail1");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(null);
            request.getVerifySignatureRequestParam().setSignature(null);
            request.getVerifySignatureRequestParam().setAlias(null);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test
    public void testVerifySignatureFail2(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail2");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(null);
            request.getVerifySignatureRequestParam().setAlias(null);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail3(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail3");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(null);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail4(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail4");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail5(final String verifySignatureEndpoint, final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail5");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput("DIFFERENT_SIGNING_INPUT");
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail6(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail6");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            // BAD_SIGNATURE
            request.getVerifySignatureRequestParam().setSignature("oQ7nIYSSAoV-lT845zRILLS2TdirRWSz978yxwK9rKzIx0vap8s7Nbqp6TsnjmtCSwisQg1kSYmg4QHNLItrfStiH3v6IpezGuo1kBnyCWj_rwsBPbnnOV6lUpFVGDIzRN4eC1A16oYQ_yJDiCfNvBGjihMw41fUYzSpK--CrvI25h2kj5tBu9TO32t-kADthsnQehqm1KNunXGR2GRnayY01MCI8EIuObd222uw1ytLIypHCAdaZDWNFv0IpuexVejmIN9l5uJkhReOsb6P_UGPRP8a5epD8DL9NsAnkWkyFBn8_9mtxYre8xxSzjIhC3p0guxZuPCnxsgKU8qtoA");
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail7(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail7");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias("WRONG_ALIAS");
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "testModeToken"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail8(final String verifySignatureEndpoint, final String signingInput,
                                         final String testModeToken) {
        try {
            showTitle("testVerifySignatureFail8");

            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setAccessToken(testModeToken);
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            // Wrong Algorithm
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test(dependsOnMethods = {"testVerifySignatureRS256", "testGenerateKeyRS256"})
    public void testDeleteKeyRS256(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyRS256");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias(rs256Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test(dependsOnMethods = {"testVerifySignatureRS384", "testGenerateKeyRS384"})
    public void testDeleteKeyRS384(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyRS384");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias(rs384Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test(dependsOnMethods = {"testVerifySignatureRS512", "testGenerateKeyRS512"})
    public void testDeleteKeyRS512(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyRS512");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias(rs512Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test(dependsOnMethods = {"testVerifySignatureES256", "testGenerateKeyES256"})
    public void testDeleteKeyES256(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyES256");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias(es256Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test(dependsOnMethods = {"testVerifySignatureES384", "testGenerateKeyES384"})
    public void testDeleteKeyES384(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyES384");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias(es384Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test(dependsOnMethods = {"testVerifySignatureES512", "testGenerateKeyES512"})
    public void testDeleteKeyES512(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyES512");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias(es512Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint", "testModeToken"})
    @Test
    public void testDeleteKeyFail(final String deleteKeyEndpoint, final String testModeToken) {
        try {
            showTitle("testDeleteKeyFail");

            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAccessToken(testModeToken);
            request.setAlias("INVALID_ALIAS");

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();

            showClient(client);
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void showTitle(String title) {
        title = "TEST: " + title;

        System.out.println("#######################################################");
        System.out.println(title);
        System.out.println("#######################################################");
    }

    public static void showClient(BaseClient client) {
        ClientUtils.showClient(client);
    }
 
}
