package io.jans.as.client;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ClientAuthnRequestTest {

    @Test
    public void getFallbackAlgorithm_whenKidIsBlank_shouldReturnHS256() {
        ClientAuthnRequest request = new TestClientAuthnRequest();
        assertEquals(request.getFallbackAlgorithm(), SignatureAlgorithm.HS256);
    }

    @Test
    public void getFallbackAlgorithm_whenKidIsNotBlank_shouldReturnRS256() {
        ClientAuthnRequest request = new TestClientAuthnRequest();
        request.setKeyId("testKid");
        assertEquals(request.getFallbackAlgorithm(), SignatureAlgorithm.RS256);
    }

    public static class TestClientAuthnRequest extends ClientAuthnRequest {
        @Override
        public String getQueryString() {
            return "";
        }
    }
}
