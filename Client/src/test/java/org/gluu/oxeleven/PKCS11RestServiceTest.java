package org.gluu.oxeleven;

import org.apache.http.HttpStatus;
import org.gluu.oxeleven.client.*;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version March 21, 2016
 */
public class PKCS11RestServiceTest {

    private final String GENERATE_KEY_ENDPOINT = "https://ce.gluu.info:8443/oxeleven/rest/oxeleven/generateKey";
    private final String SIGN_ENDPOINT = "https://ce.gluu.info:8443/oxeleven/rest/oxeleven/sign";
    private final String VERIFY_SIGNATURE_ENDPOINT = "https://ce.gluu.info:8443/oxeleven/rest/oxeleven/verifySignature";
    private final String DELETE_KEY_ENDPOINT = "https://ce.gluu.info:8443/oxeleven/rest/oxeleven/deleteKey";

    private final String SIGNING_INPUT = "Signing Input";
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

    @Test
    public void testGenerateKeyRS256() {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

            GenerateKeyClient client = new GenerateKeyClient(GENERATE_KEY_ENDPOINT);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            rs256Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGenerateKeyRS384() {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);

            GenerateKeyClient client = new GenerateKeyClient(GENERATE_KEY_ENDPOINT);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            rs384Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGenerateKeyRS512() {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);

            GenerateKeyClient client = new GenerateKeyClient(GENERATE_KEY_ENDPOINT);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            rs512Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGenerateKeyES256() {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

            GenerateKeyClient client = new GenerateKeyClient(GENERATE_KEY_ENDPOINT);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            es256Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGenerateKeyES384() {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);

            GenerateKeyClient client = new GenerateKeyClient(GENERATE_KEY_ENDPOINT);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();

            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            es384Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGenerateKeyES512() {
        try {
            GenerateKeyRequest request = new GenerateKeyRequest();
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);

            GenerateKeyClient client = new GenerateKeyClient(GENERATE_KEY_ENDPOINT);
            client.setRequest(request);

            GenerateKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getAlias());
            es512Alias = response.getAlias();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGenerateKeyRS256")
    public void testSignatureRS256() {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

            SignClient client = new SignClient(SIGN_ENDPOINT);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGenerateKeyRS384")
    public void testSignatureRS384() {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setAlias(rs384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);

            SignClient client = new SignClient(SIGN_ENDPOINT);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGenerateKeyRS512")
    public void testSignatureRS512() {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setAlias(rs512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);

            SignClient client = new SignClient(SIGN_ENDPOINT);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            rs512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGenerateKeyES256")
    public void testSignatureES256() {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setAlias(es256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

            SignClient client = new SignClient(SIGN_ENDPOINT);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es256Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGenerateKeyES384")
    public void testSignatureES384() {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setAlias(es384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);

            SignClient client = new SignClient(SIGN_ENDPOINT);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es384Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGenerateKeyES512")
    public void testSignatureES512() {
        try {
            SignRequest request = new SignRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setAlias(es512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);

            SignClient client = new SignClient(SIGN_ENDPOINT);
            client.setRequest(request);

            SignResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertNotNull(response.getSignature());
            es512Signature = response.getSignature();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testSignatureRS256")
    public void testVerifySignatureRS256() {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setSignature(rs256Signature);
            request.setAlias(rs256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS256);

            VerifySignatureClient client = new VerifySignatureClient(VERIFY_SIGNATURE_ENDPOINT);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testSignatureRS384")
    public void testVerifySignatureRS384() {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setSignature(rs384Signature);
            request.setAlias(rs384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS384);

            VerifySignatureClient client = new VerifySignatureClient(VERIFY_SIGNATURE_ENDPOINT);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testSignatureRS512")
    public void testVerifySignatureRS512() {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setSignature(rs512Signature);
            request.setAlias(rs512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.RS512);

            VerifySignatureClient client = new VerifySignatureClient(VERIFY_SIGNATURE_ENDPOINT);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testSignatureES256")
    public void testVerifySignatureES256() {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setSignature(es256Signature);
            request.setAlias(es256Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES256);

            VerifySignatureClient client = new VerifySignatureClient(VERIFY_SIGNATURE_ENDPOINT);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testSignatureES384")
    public void testVerifySignatureES384() {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setSignature(es384Signature);
            request.setAlias(es384Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES384);

            VerifySignatureClient client = new VerifySignatureClient(VERIFY_SIGNATURE_ENDPOINT);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testSignatureES512")
    public void testVerifySignatureES512() {
        try {
            VerifySignatureRequest request = new VerifySignatureRequest();
            request.setSigningInput(SIGNING_INPUT);
            request.setSignature(es512Signature);
            request.setAlias(es512Alias);
            request.setSignatureAlgorithm(SignatureAlgorithm.ES512);

            VerifySignatureClient client = new VerifySignatureClient(VERIFY_SIGNATURE_ENDPOINT);
            client.setRequest(request);

            VerifySignatureResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isVerified());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testVerifySignatureRS256")
    public void testDeleteKeyRS256() {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(rs256Alias);

            DeleteKeyClient client = new DeleteKeyClient(DELETE_KEY_ENDPOINT);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testVerifySignatureRS384")
    public void testDeleteKeyRS384() {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(rs384Alias);

            DeleteKeyClient client = new DeleteKeyClient(DELETE_KEY_ENDPOINT);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testVerifySignatureRS512")
    public void testDeleteKeyRS512() {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(rs512Alias);

            DeleteKeyClient client = new DeleteKeyClient(DELETE_KEY_ENDPOINT);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testVerifySignatureES256")
    public void testDeleteKeyES256() {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(es256Alias);

            DeleteKeyClient client = new DeleteKeyClient(DELETE_KEY_ENDPOINT);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testVerifySignatureES384")
    public void testDeleteKeyES384() {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(es384Alias);

            DeleteKeyClient client = new DeleteKeyClient(DELETE_KEY_ENDPOINT);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testVerifySignatureES512")
    public void testDeleteKeyES512() {
        try {
            DeleteKeyRequest request = new DeleteKeyRequest();
            request.setAlias(es512Alias);

            DeleteKeyClient client = new DeleteKeyClient(DELETE_KEY_ENDPOINT);
            client.setRequest(request);

            DeleteKeyResponse response = client.exec();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            assertTrue(response.isDeleted());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
