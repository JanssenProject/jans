/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven;

import org.apache.http.HttpStatus;
import org.gluu.oxeleven.client.*;
import org.gluu.oxeleven.model.Jwks;
import org.gluu.oxeleven.model.Key;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class PKCS11RestServiceTest {

    private String rs256Alias;
    private String rs384Alias;
    private String rs512Alias;
    private String es256Alias;
    private String es384Alias;
    //private String es512Alias;
    private String rs256Signature;
    private String rs384Signature;
    private String rs512Signature;
    private String es256Signature;
    private String es384Signature;
    //private String es512Signature;

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyRS256(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            rs256Alias = response.getAlias();
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

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            rs384Alias = response.getAlias();
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

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            rs512Alias = response.getAlias();
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

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            es256Alias = response.getAlias();
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

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            es384Alias = response.getAlias();
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

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            es512Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }*/

    @Parameters({"generateKeyEndpoint"})
    @Test
    public void testGenerateKeyFail(final String generateKeyEndpoint) {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(null);

            GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"signEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureRS256(final String signEndpoint, final String signingInput) {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(signingInput);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setAlias(rs384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);

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
            request.setSigningInput(signingInput);
            request.setAlias(rs512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);

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
            request.setSigningInput(signingInput);
            request.setAlias(es256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

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
            request.setSigningInput(signingInput);
            request.setAlias(es384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);

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
            request.setSigningInput(signingInput);
            request.setAlias(es512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);

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
            request.setSigningInput(null);
            request.setAlias(null);
            request.setSignatureAlgorithm(null);

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
            request.setSigningInput(null);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setAlias(null);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setAlias("INVALID_ALIAS");
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

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
            request.setSigningInput(signingInput);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(null);

            SignClient client = new SignClient(signEndpoint);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"verifySignatureEndpoint", "signingInput"})
    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureRS256(final String verifySignatureEndpoint, final String signingInput) {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(signingInput);
            request.setSignature(rs256Signature);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setSignature(rs384Signature);
            request.setAlias(rs384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);

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
            request.setSigningInput(signingInput);
            request.setSignature(rs512Signature);
            request.setAlias(rs512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);

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
            request.setSigningInput(signingInput);
            request.setSignature(es256Signature);
            request.setAlias(es256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

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
            request.setSigningInput(signingInput);
            request.setSignature(es384Signature);
            request.setAlias(es384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);

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
            request.setSigningInput(signingInput);
            request.setSignature(es512Signature);
            request.setAlias(es512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);

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
            request.setSigningInput(null);
            request.setSignature(null);
            request.setAlias(null);
            request.setSignatureAlgorithm(null);

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
            request.setSigningInput(signingInput);
            request.setSignature(null);
            request.setAlias(null);
            request.setSignatureAlgorithm(null);

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
            request.setSigningInput(signingInput);
            request.setSignature(rs256Signature);
            request.setAlias(null);
            request.setSignatureAlgorithm(null);

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
            request.setSigningInput(signingInput);
            request.setSignature(rs256Signature);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(null);

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
            request.setSigningInput("DIFFERENT_SIGNING_INPUT");
            request.setSignature(rs256Signature);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            // BAD_SIGNATURE
            request.setSignature("oQ7nIYSSAoV-lT845zRILLS2TdirRWSz978yxwK9rKzIx0vap8s7Nbqp6TsnjmtCSwisQg1kSYmg4QHNLItrfStiH3v6IpezGuo1kBnyCWj_rwsBPbnnOV6lUpFVGDIzRN4eC1A16oYQ_yJDiCfNvBGjihMw41fUYzSpK--CrvI25h2kj5tBu9TO32t-kADthsnQehqm1KNunXGR2GRnayY01MCI8EIuObd222uw1ytLIypHCAdaZDWNFv0IpuexVejmIN9l5uJkhReOsb6P_UGPRP8a5epD8DL9NsAnkWkyFBn8_9mtxYre8xxSzjIhC3p0guxZuPCnxsgKU8qtoA");
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setSignature(rs256Signature);
            request.setAlias("WRONG_ALIAS");
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

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
            request.setSigningInput(signingInput);
            request.setSignature(rs256Signature);
            request.setAlias(rs256Alias);
            // Wrong Algorithm
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

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
            Key k1 = new Key("RSA", "sig", SignatureAlgorithm.RS256, rs256Alias);
            Key k2 = new Key("RSA", "sig", SignatureAlgorithm.RS384, rs384Alias);
            Key k3 = new Key("RSA", "sig", SignatureAlgorithm.RS512, rs512Alias);
            Key k4 = new Key("EC", "sig", SignatureAlgorithm.ES256, "P-256", es256Alias);
            Key k5 = new Key("EC", "sig", SignatureAlgorithm.ES384, "P-384", es384Alias);
            //Key k6 = new Key("EC", "sig", SignatureAlgorithm.ES512, "P-521", es512Alias);

            Jwks jwks = new Jwks();
            jwks.setKeys(Arrays.asList(k1, k2, k3, k4, k5/*, k6*/));

            JwksRequest request = new JwksRequest();
            request.setJwks(jwks);

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getJwks());
            assertNotNull(response.getJwks().getKeys());
            assertEquals(response.getJwks().getKeys().size(), 5);
            for (Key key : response.getJwks().getKeys()) {
                assertNotNull(key.getAlg());
                assertNotNull(key.getKid());
                assertNotNull(key.getUse());
                assertNotNull(key.getKty());
                if (key.getKty().equals("RSA")) {
                    assertNotNull(key.getN());
                    assertNotNull(key.getE());
                } else if (key.getKty().equals("EC")) {
                    assertNotNull(key.getCrv());
                    assertNotNull(key.getX());
                    assertNotNull(key.getY());
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
            JwksRequest request = new JwksRequest();

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_BAD_REQUEST);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"jwksEndpoint"})
    @Test(dependsOnMethods = {"testGenerateKeyRS256", "testGenerateKeyRS384", "testGenerateKeyRS512",
            "testGenerateKeyES256", "testGenerateKeyES384"/*, "testGenerateKeyES512"*/})
    public void testJwks3(final String jwksEndpoint) {
        try {
            Key k1 = new Key("RSA", "sig", SignatureAlgorithm.RS256, "INVALID_ALIAS_1");
            Key k2 = new Key("RSA", "sig", SignatureAlgorithm.RS384, "INVALID_ALIAS_2");
            Key k3 = new Key("RSA", "sig", SignatureAlgorithm.RS512, "INVALID_ALIAS_3");
            Key k4 = new Key("EC", "sig", SignatureAlgorithm.ES256, "P-256", "INVALID_ALIAS_4");
            Key k5 = new Key("EC", "sig", SignatureAlgorithm.ES384, "P-384", "INVALID_ALIAS_5");
            //Key k6 = new Key("EC", "sig", SignatureAlgorithm.ES512, "P-521", "INVALID_ALIAS_6");

            Jwks jwks = new Jwks();
            jwks.setKeys(Arrays.asList(k1, k2, k3, k4, k5/*, k6*/));

            JwksRequest request = new JwksRequest();
            request.setJwks(jwks);

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
            Key k1 = new Key("RSA", "sig", SignatureAlgorithm.RS256, "INVALID_ALIAS_1");
            Key k2 = new Key("RSA", "sig", SignatureAlgorithm.RS256, rs256Alias);
            Key k3 = new Key("RSA", "sig", SignatureAlgorithm.RS384, rs384Alias);
            Key k4 = new Key("RSA", "sig", SignatureAlgorithm.RS512, rs512Alias);
            Key k5 = new Key("EC", "sig", SignatureAlgorithm.ES256, "P-256", "INVALID_ALIAS_2");
            Key k6 = new Key("EC", "sig", SignatureAlgorithm.ES256, "P-256", es256Alias);
            Key k7 = new Key("EC", "sig", SignatureAlgorithm.ES384, "P-384", es384Alias);
            //Key k8 = new Key("EC", "sig", SignatureAlgorithm.ES512, "P-521", es512Alias);

            Jwks jwks = new Jwks();
            jwks.setKeys(Arrays.asList(k1, k2, k3, k4, k5, k6, k7/*, k8*/));

            JwksRequest request = new JwksRequest();
            request.setJwks(jwks);

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getJwks());
            assertNotNull(response.getJwks().getKeys());
            assertEquals(response.getJwks().getKeys().size(), 5);
            for (Key key : response.getJwks().getKeys()) {
                assertNotNull(key.getAlg());
                assertNotNull(key.getKid());
                assertNotNull(key.getUse());
                assertNotNull(key.getKty());
                if (key.getKty().equals("RSA")) {
                    assertNotNull(key.getN());
                    assertNotNull(key.getE());
                } else if (key.getKty().equals("EC")) {
                    assertNotNull(key.getCrv());
                    assertNotNull(key.getX());
                    assertNotNull(key.getY());
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
