package org.gluu.oxeleven;

import org.apache.http.HttpStatus;
import org.gluu.oxeleven.client.*;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class PKCS11RestServiceTest {

    private String rs256Alias;
    private String rs384Alias;
    private String rs512Alias;
    private String es256Alias;
    private String es384Alias;
    private String es512Alias;
    private String rs256Signature;
    private String rs384Signature;
    private String rs512Signature;
    private String es256Signature;
    private String es384Signature;
    private String es512Signature;

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

    @Parameters({"generateKeyEndpoint"})
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

    @Parameters({"signEndpoint", "signingInput"})
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

    @Parameters({"verifySignatureEndpoint", "signingInput"})
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
    }

    @Parameters({"jwksEndpoint"})
    @Test(dependsOnMethods = {"testGenerateKeyRS256", "testGenerateKeyRS384", "testGenerateKeyRS512",
            "testGenerateKeyES256", "testGenerateKeyES384"/*, "testGenerateKeyES512"*/})
    public void testJwks(final String jwksEndpoint) {
        try {
            JwksRequest request = new JwksRequest();
            request.setAliasList(Arrays.asList(
                    rs256Alias, rs384Alias, rs512Alias,
                    es256Alias, es384Alias, es512Alias));

            JwksClient client = new JwksClient(jwksEndpoint);
            client.setRequest(request);

            JwksResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureRS256", "testJwks"})
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
    @Test(dependsOnMethods = {"testVerifySignatureRS384", "testJwks"})
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
    @Test(dependsOnMethods = {"testVerifySignatureRS512", "testJwks"})
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
    @Test(dependsOnMethods = {"testVerifySignatureES256", "testJwks"})
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
    @Test(dependsOnMethods = {"testVerifySignatureES384", "testJwks"})
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

    @Parameters({"deleteKeyEndpoint"})
    @Test(dependsOnMethods = {"testVerifySignatureES512", "testJwks"})
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
    }
}
