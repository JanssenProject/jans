/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven;

import org.apache.http.HttpStatus;
import org.gluu.oxeleven.client.*;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.gluu.oxeleven.model.KeyRequestParam;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version April 27, 2016
 */
public class PKCS11RestServiceTest {

    private Long expirationTime;
    private String rs256Alias;
    private String rs384Alias;
    private String rs512Alias;
    private String es256Alias;
    private String es384Alias;
    //private String es512Alias;
    private String noneSignature;
    private String hs256Signature;
    private String hs384Signature;
    private String hs512Signature;
    private String rs256Signature;
    private String rs384Signature;
    private String rs512Signature;
    private String es256Signature;
    private String es384Signature;
    //private String es512Signature;

    @BeforeSuite
    public void init() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(GregorianCalendar.MINUTE, 5);
        expirationTime = calendar.getTimeInMillis();
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyRS256(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            rs256Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyRS384(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            rs384Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyRS512(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            rs512Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyES256(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            es256Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyES384(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            es384Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*@Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyES512(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getKeyId());
            es512Alias = response.getKeyId();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }*/

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail1(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(null);
            request.setExpirationTime(null);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail2(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(null);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail3(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
            request.setExpirationTime(null);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail4(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.NONE);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail5(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.HS256);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail6(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.HS384);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail7(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.HS512);
            request.setExpirationTime(expirationTime);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test
    public void testSignatureNone(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.NONE);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertEquals("", response.getSignature());
            noneSignature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testSignatureHS256(final String signEndpoint, final String signingInput, final String sharedSecret) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            hs256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testSignatureHS384(final String signEndpoint, final String signingInput, final String sharedSecret) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS384);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            hs384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput", "sharedSecret"})
    @Test
    public void testSignatureHS512(final String signEndpoint, final String signingInput, final String sharedSecret) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS512);
            request.getSignRequestParam().setSharedSecret(sharedSecret);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            hs512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureRS256(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS384")
    public void testSignatureRS384(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs384Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS384);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS512")
    public void testSignatureRS512(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs512Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS512);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyES256")
    public void testSignatureES256(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(es256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyES384")
    public void testSignatureES384(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(es384Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES384);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*@Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyES512")
    public void testSignatureES512(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(es512Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES512);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }*/

    @Parameters({"signEndpoint"})
    @Test
    public void testSignatureFail1(final String signEndpoint) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(null);
            request.getSignRequestParam().setAlias(null);
            request.getSignRequestParam().setSignatureAlgorithm(null);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail2(final String signEndpoint) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(null);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail3(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(null);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test
    public void testSignatureFail4(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias("INVALID_ALIAS");
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail5(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureFail6(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.getSignRequestParam().setSigningInput(signingInput);
            request.getSignRequestParam().setAlias(rs256Alias);
            request.getSignRequestParam().setSignatureAlgorithm(null);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureNone")
    public void testVerifySignatureNone(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(noneSignature);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.NONE);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret"})
    @Test(dependsOnMethods = "testSignatureHS256")
    public void testVerifySignatureHS256(final String verifySignatureEndpoint, final String signingInput, final String sharedSecret) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs256Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret"})
    @Test(dependsOnMethods = "testSignatureHS384")
    public void testVerifySignatureHS384(final String verifySignatureEndpoint, final String signingInput, final String sharedSecret) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs384Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput", "sharedSecret"})
    @Test(dependsOnMethods = "testSignatureHS512")
    public void testVerifySignatureHS512(final String verifySignatureEndpoint, final String signingInput, final String sharedSecret) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(hs512Signature);
            request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureRS256(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureRS256Jwks(final String verifySignatureEndpoint) {
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
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS384")
    public void testVerifySignatureRS384(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs384Signature);
            request.getVerifySignatureRequestParam().setAlias(rs384Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureRS384Jwks(final String verifySignatureEndpoint) {
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
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS512")
    public void testVerifySignatureRS512(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs512Signature);
            request.getVerifySignatureRequestParam().setAlias(rs512Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureRS512Jwks(final String verifySignatureEndpoint) {
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
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureES256")
    public void testVerifySignatureES256(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(es256Signature);
            request.getVerifySignatureRequestParam().setAlias(es256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureES256Jwks(final String verifySignatureEndpoint) {
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
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureES384")
    public void testVerifySignatureES384(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(es384Signature);
            request.getVerifySignatureRequestParam().setAlias(es384Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureES384Jwks(final String verifySignatureEndpoint) {
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
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES384);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*@Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureES512")
    public void testVerifySignatureES512(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(es512Signature);
            request.getVerifySignatureRequestParam().setAlias(es512Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }*/

    /*@Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureES512Jwks(final String verifySignatureEndpoint) {
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
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(signature);
            request.getVerifySignatureRequestParam().setAlias(alias);
            request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES512);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }*/

    @Parameters({"verifySignatureEndpoint"})
    @Test
    public void testVerifySignatureFail1(final String verifySignatureEndpoint) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(null);
            request.getVerifySignatureRequestParam().setSignature(null);
            request.getVerifySignatureRequestParam().setAlias(null);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test
    public void testVerifySignatureFail2(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(null);
            request.getVerifySignatureRequestParam().setAlias(null);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail3(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(null);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail4(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(null);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail5(final String verifySignatureEndpoint) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput("DIFFERENT_SIGNING_INPUT");
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail6(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            // BAD_SIGNATURE
            request.getVerifySignatureRequestParam().setSignature("oQ7nIYSSAoV-lT845zRILLS2TdirRWSz978yxwK9rKzIx0vap8s7Nbqp6TsnjmtCSwisQg1kSYmg4QHNLItrfStiH3v6IpezGuo1kBnyCWj_rwsBPbnnOV6lUpFVGDIzRN4eC1A16oYQ_yJDiCfNvBGjihMw41fUYzSpK--CrvI25h2kj5tBu9TO32t-kADthsnQehqm1KNunXGR2GRnayY01MCI8EIuObd222uw1ytLIypHCAdaZDWNFv0IpuexVejmIN9l5uJkhReOsb6P_UGPRP8a5epD8DL9NsAnkWkyFBn8_9mtxYre8xxSzjIhC3p0guxZuPCnxsgKU8qtoA");
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail7(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias("WRONG_ALIAS");
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureFail8(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.getVerifySignatureRequestParam().setSigningInput(signingInput);
            request.getVerifySignatureRequestParam().setSignature(rs256Signature);
            request.getVerifySignatureRequestParam().setAlias(rs256Alias);
            // Wrong Algorithm
            request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"jwksEndpoint"})
    @Test(dependsOnMethods = {"testGenerateKeyRS256", "testGenerateKeyRS384", "testGenerateKeyRS512",
            "testGenerateKeyES256", "testGenerateKeyES384"/*, "testGenerateKeyES512"*/})
    public void testJwks1(final String jwksEndpoint) {
        try {
            KeyRequestParam k1 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS256, rs256Alias);
            KeyRequestParam k2 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS384, rs384Alias);
            KeyRequestParam k3 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS512, rs512Alias);
            KeyRequestParam k4 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES256, "P-256", es256Alias);
            KeyRequestParam k5 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES384, "P-384", es384Alias);
            //Key k6 = new Key("EC", "sig", SignatureAlgorithm.ES512, "P-521", es512Alias);

            JwksRequestParam jwksRequestParam = new JwksRequestParam();
            jwksRequestParam.setKeyRequestParams(Arrays.asList(k1, k2, k3, k4, k5/*, k6*/));

            JwksRequest request = new JwksRequest();
            request.setJwksRequestParam(jwksRequestParam);

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getJwksRequestParam());
            assertNotNull(response.getJwksRequestParam().getKeyRequestParams());
            assertEquals(response.getJwksRequestParam().getKeyRequestParams().size(), 5);
            for (KeyRequestParam keyRequestParam : response.getJwksRequestParam().getKeyRequestParams()) {
                assertNotNull(keyRequestParam.getAlg());
                assertNotNull(keyRequestParam.getKid());
                assertNotNull(keyRequestParam.getUse());
                assertNotNull(keyRequestParam.getKty());
                if (keyRequestParam.getKty().equals("RSA")) {
                    assertNotNull(keyRequestParam.getN());
                    assertNotNull(keyRequestParam.getE());
                } else if (keyRequestParam.getKty().equals("EC")) {
                    assertNotNull(keyRequestParam.getCrv());
                    assertNotNull(keyRequestParam.getX());
                    assertNotNull(keyRequestParam.getY());
                } else {
                    fail("Wrong Key Type");
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"jwksEndpoint"})
    @Test(dependsOnMethods = {"testGenerateKeyRS256", "testGenerateKeyRS384", "testGenerateKeyRS512",
            "testGenerateKeyES256", "testGenerateKeyES384"/*, "testGenerateKeyES512"*/})
    public void testJwks2(final String jwksEndpoint) {
        try {
            JwksRequestParam jwksRequestParam = new JwksRequestParam();
            jwksRequestParam.setKeyRequestParams(new ArrayList<KeyRequestParam>());

            JwksRequest request = new JwksRequest();
            request.setJwksRequestParam(jwksRequestParam);

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getJwksRequestParam());
            assertNotNull(response.getJwksRequestParam().getKeyRequestParams());
            assertEquals(response.getJwksRequestParam().getKeyRequestParams().size(), 0);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"jwksEndpoint"})
    @Test(dependsOnMethods = {"testGenerateKeyRS256", "testGenerateKeyRS384", "testGenerateKeyRS512",
            "testGenerateKeyES256", "testGenerateKeyES384"/*, "testGenerateKeyES512"*/})
    public void testJwks3(final String jwksEndpoint) {
        try {
            KeyRequestParam k1 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS256, "INVALID_ALIAS_1");
            KeyRequestParam k2 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS384, "INVALID_ALIAS_2");
            KeyRequestParam k3 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS512, "INVALID_ALIAS_3");
            KeyRequestParam k4 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES256, "P-256", "INVALID_ALIAS_4");
            KeyRequestParam k5 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES384, "P-384", "INVALID_ALIAS_5");
            //Key k6 = new Key("EC", "sig", SignatureAlgorithm.ES512, "P-521", "INVALID_ALIAS_6");

            JwksRequestParam jwksRequestParam = new JwksRequestParam();
            jwksRequestParam.setKeyRequestParams(Arrays.asList(k1, k2, k3, k4, k5/*, k6*/));

            JwksRequest request = new JwksRequest();
            request.setJwksRequestParam(jwksRequestParam);

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"jwksEndpoint"})
    @Test(dependsOnMethods = {"testGenerateKeyRS256", "testGenerateKeyRS384", "testGenerateKeyRS512",
            "testGenerateKeyES256", "testGenerateKeyES384"/*, "testGenerateKeyES512"*/})
    public void testJwks4(final String jwksEndpoint) {
        try {
            KeyRequestParam k1 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS256, "INVALID_ALIAS_1");
            KeyRequestParam k2 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS256, rs256Alias);
            KeyRequestParam k3 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS384, rs384Alias);
            KeyRequestParam k4 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS512, rs512Alias);
            KeyRequestParam k5 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES256, "P-256", "INVALID_ALIAS_2");
            KeyRequestParam k6 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES256, "P-256", es256Alias);
            KeyRequestParam k7 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES384, "P-384", es384Alias);
            //Key k8 = new Key("EC", "sig", SignatureAlgorithm.ES512, "P-521", es512Alias);

            JwksRequestParam jwksRequestParam = new JwksRequestParam();
            jwksRequestParam.setKeyRequestParams(Arrays.asList(k1, k2, k3, k4, k5, k6, k7/*, k8*/));

            JwksRequest request = new JwksRequest();
            request.setJwksRequestParam(jwksRequestParam);

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getJwksRequestParam());
            assertNotNull(response.getJwksRequestParam().getKeyRequestParams());
            assertEquals(response.getJwksRequestParam().getKeyRequestParams().size(), 5);
            for (KeyRequestParam keyRequestParam : response.getJwksRequestParam().getKeyRequestParams()) {
                assertNotNull(keyRequestParam.getAlg());
                assertNotNull(keyRequestParam.getKid());
                assertNotNull(keyRequestParam.getUse());
                assertNotNull(keyRequestParam.getKty());
                if (keyRequestParam.getKty().equals("RSA")) {
                    assertNotNull(keyRequestParam.getN());
                    assertNotNull(keyRequestParam.getE());
                } else if (keyRequestParam.getKty().equals("EC")) {
                    assertNotNull(keyRequestParam.getCrv());
                    assertNotNull(keyRequestParam.getX());
                    assertNotNull(keyRequestParam.getY());
                } else {
                    fail("Wrong Key Type");
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureRS256", "testJwks1", "testJwks2", "testJwks3", "testJwks4"})
    public void testDeleteKeyRS256(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(rs256Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureRS384", "testJwks1", "testJwks2", "testJwks3", "testJwks4"})
    public void testDeleteKeyRS384(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(rs384Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureRS512", "testJwks1", "testJwks2", "testJwks3", "testJwks4"})
    public void testDeleteKeyRS512(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(rs512Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureES256", "testJwks1", "testJwks2", "testJwks3", "testJwks4"})
    public void testDeleteKeyES256(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(es256Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureES384", "testJwks1", "testJwks2", "testJwks3", "testJwks4"})
    public void testDeleteKeyES384(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(es384Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*@Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureES512", "testJwks1", "testJwks2", "testJwks3", "testJwks4"})
    public void testDeleteKeyES512(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(es512Alias);

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }*/

    @Parameters({"deleteKeyEndpoint"})
    @Test
    public void testDeleteKeyFail(final String deleteKeyEndpoint) {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias("INVALID_ALIAS");

            DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertFalse(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
