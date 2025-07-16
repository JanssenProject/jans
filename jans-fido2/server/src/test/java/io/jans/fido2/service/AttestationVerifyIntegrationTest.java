package io.jans.fido2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.service.operation.AttestationServiceClient;
import io.jans.util.BaseFido2IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class AttestationVerifyIntegrationTest extends BaseFido2IntegrationTest {

    private AttestationServiceClient attestationService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        loadTestProperties();

        String fido2ConfigUrl = "https://" + getServerHost() + "/.well-known/fido2-configuration";
//        String fido2ConfigUrl = "https://imran-ishaq-capable-cobra.gluu.info/.well-known/fido2-configuration";
        JsonNode config = mapper.readTree(new URL(fido2ConfigUrl));

        String basePath = config.get("attestation").get("base_path").asText();
        System.out.println("🔍 Resolved attestation base path: " + basePath);

        attestationService = RSUtils.getClient()
                .target(basePath)
                .proxy(AttestationServiceClient.class);
    }

    @Test
    void testVerifyAttestationResponse() throws Exception {
        String tokenResponseJson = "{"
                + "\"id\":\"aGd1-r0BqD5m06jXRKBWsvp3mZNGGu0IcvUsIF3gqeIC02HdspbS8iaywTjmaeV2ziQy6SPgj_WRAtDCYRA-BQ\","
                + "\"rawId\":\"aGd1-r0BqD5m06jXRKBWsvp3mZNGGu0IcvUsIF3gqeIC02HdspbS8iaywTjmaeV2ziQy6SPgj_WRAtDCYRA-BQ\","
                + "\"type\":\"public-key\","
                + "\"authenticatorAttachment\":\"cross-platform\","
                + "\"clientExtensionResults\":{},"
                + "\"response\":{"
                + "\"attestationObject\":\"o2NmbXRoZmlkby11MmZnYXR0U3RtdKJjc2lnWEgwRgIhAI2pJ9FuP-RXovF6x6Mav8hA4Df87_qhqOkkvwv7OUAoAiEAlk82eTMndIYU_gns5a9B49kyg5TlRX5fL1gTTBf4oS1jeDVjgVkC3TCCAtkwggHBoAMCAQICCQDxLMXBIyNjzjANBgkqhkiG9w0BAQsFADAuMSwwKgYDVQQDEyNZdWJpY28gVTJGIFJvb3QgQ0EgU2VyaWFsIDQ1NzIwMDYzMTAgFw0xNDA4MDEwMDAwMDBaGA8yMDUwMDkwNDAwMDAwMFowbzELMAkGA1UEBhMCU0UxEjAQBgNVBAoMCVl1YmljbyBBQjEiMCAGA1UECwwZQXV0aGVudGljYXRvciBBdHRlc3RhdGlvbjEoMCYGA1UEAwwfWXViaWNvIFUyRiBFRSBTZXJpYWwgMTEwMzQ0MTEzNzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABITS94t52AZfFVc89JYOIgpy-u-41wtWJ0faukeWi5ds1JzTLNguoo4iltWlwLW4YxNkxgDVAEK_1D_wCLA35cujgYEwfzATBgorBgEEAYLECg0BBAUEAwUEAzAiBgkrBgEEAYLECgIEFTEuMy42LjEuNC4xLjQxNDgyLjEuNzATBgsrBgEEAYLlHAIBAQQEAwIEMDAhBgsrBgEEAYLlHAEBBAQSBBAvwFefgRNH6rEWu1qNuSAqMAwGA1UdEwEB_wQCMAAwDQYJKoZIhvcNAQELBQADggEBAH2R3fEMW0TGdgKCphKbcRDpUvAXsCqVUpHt9DQydwNRyQyyM-AI-PQ_ORArQ-hdN5s3xGvEzUHGgxLzqH135fkTe8pl4eXuoaWQHszCWMyWP9xvD7U_n8vbT_6N7MzE6Ywp4JXM2ybRlpD_t7BNohTNf6D7hc8GHcICcI4gVoU03U1uw6WVY-AfW1IkUdeJYLFxoKQMDmQBPTuNcC-vzAtpCc7u_VOAxnt4TidNTI2PHLR7HfMUvzg6rSEy3s6ms4rUOa4lqHxMGN8mBF8hP-bqkXcdo00hIgeO4YLqqDsNI6qS3C9D_GKaoGdFIymUfeM_8SgiXP21S1QKi0mpPfhoYXV0aERhdGFYxCfcLyk7t5hNLCHUNcWjIaViM-GbXK8u-vfZgUeOAREbQQAAAAAAAAAAAAAAAAAAAAAAAAAAAEBoZ3X6vQGoPmbTqNdEoFay-neZk0Ya7Qhy9SwgXeCp4gLTYd2yltLyJrLBOOZp5XbOJDLpI-CP9ZEC0MJhED4FpQECAyYgASFYIK-on0uXFrfJ8yZO5jL4-EhI7DeDx6_3N0_9CSie3Xk-Ilggdl0rGTGAG1XlnSIkT_e2nYUOvmEYm7OKZUBetKJzvMQ\","
                + "\"authenticatorData\":\"J9wvKTu3mE0sIdQ1xaMhpWIz4Ztcry7699mBR44BERtBAAAAAAAAAAAAAAAAAAAAAAAAAAAAQGhndfq9Aag-ZtOo10SgVrL6d5mTRhrtCHL1LCBd4KniAtNh3bKW0vImssE45mnlds4kMukj4I_1kQLQwmEQPgWlAQIDJiABIVggr6ifS5cWt8nzJk7mMvj4SEjsN4PHr_c3T_0JKJ7deT4iWCB2XSsZMYAbVeWdIiRP97adhQ6-YRibs4plQF60onO8xA\","
                + "\"clientDataJSON\":\"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiWHQ2SmZHSTBuM2JfNk95UUVjNTJEMmd5SUVVZ19oV3RlNnFOaUttRUhOdyIsIm9yaWdpbiI6Imh0dHBzOi8vaW1yYW4taXNoYXEtY2FwYWJsZS1jb2JyYS5nbHV1LmluZm8iLCJjcm9zc09yaWdpbiI6ZmFsc2V9\","
                + "\"publicKey\":\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEr6ifS5cWt8nzJk7mMvj4SEjsN4PHr_c3T_0JKJ7deT52XSsZMYAbVeWdIiRP97adhQ6-YRibs4plQF60onO8xA\","
                + "\"publicKeyAlgorithm\":-7,"
                + "\"transports\":[\"nfc\",\"usb\"]"
                + "}"
                + "}";

        JsonNode tokenNode = mapper.readTree(tokenResponseJson);
        AttestationResult result = mapper.convertValue(tokenNode, AttestationResult.class);

        System.out.println("⏩ Sending payload: " + mapper.writeValueAsString(result));

        AttestationOrAssertionResponse response = attestationService.verify(result);

        assertNotNull(response);
        System.out.println("✅ Verification successful.");
        System.out.println("Response: " + mapper.writeValueAsString(response));
    }
}
