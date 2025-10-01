package io.jans.as.server.register.ws.rs;

import io.jans.as.client.RegisterRequest;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.ciba.CIBARegisterParamsValidatorService;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.as.server.service.net.UriService;
import jakarta.ws.rs.WebApplicationException;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RegisterValidatorTest {

    @InjectMocks
    @Spy
    private RegisterValidator registerValidator;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private CIBARegisterParamsValidatorService cibaRegisterParamsValidatorService;

    @Mock
    private RegisterParamsValidator registerParamsValidator;

    @Mock
    private SsaValidationConfigService ssaValidationConfigService;

    @Mock
    private UriService uriService;

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateEvidence_whenAbsentButRequired_shouldReturnError() {
        when(appConfiguration.getDcrAttestationEvidenceRequired()).thenReturn(true);
        when(errorResponseFactory.errorAsJson(any(), any())).thenReturn("{}");

        registerValidator.validateEvidence(new RegisterRequest());
    }

    @Test
    public void validateEvidence_whenAbsentAndNotRequired_shouldNotRaiseError() {
        when(appConfiguration.getDcrAttestationEvidenceRequired()).thenReturn(false);

        registerValidator.validateEvidence(new RegisterRequest());
    }

    @Test
    public void validateEvidence_whenPresentAndRequired_shouldNotRaiseError() {
        when(appConfiguration.getDcrAttestationEvidenceRequired()).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEvidence("custom_evidence");
        registerValidator.validateEvidence(registerRequest);
    }

    @Test
    public void validateIssuer_whenTrustedIssuersAreNotConfigured_shouldPass() {
        when(appConfiguration.getTrustedSsaIssuers()).thenReturn(new HashMap<>());

        final JSONObject ssa = createSsaPayload();
        registerValidator.validateIssuer(ssa);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIssuer_whenTrustedIssuersDoesNotMatch_shouldFail() {
        final HashMap<String, TrustedIssuerConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put("https://some.com", new TrustedIssuerConfig());

        when(appConfiguration.getTrustedSsaIssuers()).thenReturn(trustedIssuers);
        when(errorResponseFactory.createWebApplicationException(any(), any(), anyString())).thenReturn(new WebApplicationException());

        final JSONObject ssa = createSsaPayload();
        registerValidator.validateIssuer(ssa);
    }

    @Test
    public void validateIssuer_whenTrustedIssuerMatch_shouldPass() {
        final HashMap<String, TrustedIssuerConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put("https://trusted.as.com", new TrustedIssuerConfig());

        when(appConfiguration.getTrustedSsaIssuers()).thenReturn(trustedIssuers);

        final JSONObject ssa = createSsaPayload();
        registerValidator.validateIssuer(ssa);
    }

    @Test
    public void applyTrustedIssuerConfig_forNull_shouldPass() {
        final JSONObject ssa = createSsaPayload();
        registerValidator.applyTrustedIssuerConfig(null, ssa);
    }

    @Test
    public void applyTrustedIssuerConfig_forTrustedConfigWithScopes_shouldApplyScopes() {
        final TrustedIssuerConfig trustedIssuerConfig = new TrustedIssuerConfig();
        trustedIssuerConfig.setAutomaticallyGrantedScopes(Arrays.asList("b", "c"));

        final JSONObject ssa = createSsaPayload();
        registerValidator.applyTrustedIssuerConfig(trustedIssuerConfig, ssa);

        assertEquals("a b c", ssa.getString("scope"));
    }

    @Test
    public void applyTrustedIssuerConfig_forTrustedConfigWithoutScopes_shouldGetNoChangesForScopesInSsa() {
        final TrustedIssuerConfig trustedIssuerConfig = new TrustedIssuerConfig();
        trustedIssuerConfig.setAutomaticallyGrantedScopes(new ArrayList<>());

        final JSONObject ssa = createSsaPayload();
        registerValidator.applyTrustedIssuerConfig(trustedIssuerConfig, ssa);

        assertEquals("a b", ssa.getString("scope"));
    }

    private JSONObject createSsaPayload() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("iss", "https://trusted.as.com");
        jsonObject.put("scope", "a b");
        return jsonObject;
    }
}
