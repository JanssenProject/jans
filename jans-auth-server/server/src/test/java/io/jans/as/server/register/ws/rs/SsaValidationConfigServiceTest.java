package io.jans.as.server.register.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.ssa.SsaValidationConfig;
import io.jans.as.model.ssa.SsaValidationType;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class SsaValidationConfigServiceTest {

    @InjectMocks
    @Spy
    private SsaValidationConfigService ssaValidationConfigService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @BeforeMethod
    public void setUp() {
        SsaValidationConfig one = new SsaValidationConfig();
        one.setId(UUID.randomUUID().toString());
        one.setType(SsaValidationType.DCR);
        one.setIssuers(Collections.singletonList("Acme"));
        one.setScopes(Arrays.asList("read", "write"));
        one.setAllowedClaims(Arrays.asList("exp", "iat"));
        one.setJwks("{}");
        one.setSharedSecret("secret");

        SsaValidationConfig two = new SsaValidationConfig();
        two.setId(UUID.randomUUID().toString());
        two.setType(SsaValidationType.SSA);
        two.setIssuers(Collections.singletonList("jans-auth"));
        two.setScopes(Arrays.asList("my_read", "my_write"));
        two.setAllowedClaims(Arrays.asList("test_exp", "test_iat"));
        two.setJwks("{}");
        two.setSharedSecret("secret");

        SsaValidationConfig three = new SsaValidationConfig();
        three.setId(UUID.randomUUID().toString());
        three.setType(SsaValidationType.SSA);
        three.setIssuers(Collections.singletonList("empty"));
        three.setJwks("{}");
        three.setSharedSecret("secret");

        lenient().when(appConfiguration.getDcrSsaValidationConfigs()).thenReturn(Arrays.asList(
                one, two, three
        ));
    }

    @Test
    public void getByIssuer_whenCalledWithWrongIssuer_shouldReturnEmptyList() {
        List<SsaValidationConfig> configs = ssaValidationConfigService.getByIssuer("none_existent_issuer", SsaValidationType.DCR);
        assertTrue(configs.isEmpty());

        configs = ssaValidationConfigService.getByIssuer("none_existent_issuer", SsaValidationType.SSA);
        assertTrue(configs.isEmpty());

        configs = ssaValidationConfigService.getByIssuer("none_existent_issuer", SsaValidationType.NONE);
        assertTrue(configs.isEmpty());

        configs = ssaValidationConfigService.getByIssuer("none_existent_issuer", null);
        assertTrue(configs.isEmpty());
    }

    @Test
    public void getByIssuer_whenCalledExistingIssuer_shouldReturnNonEmptyList() {
        List<SsaValidationConfig> configs = ssaValidationConfigService.getByIssuer("Acme", SsaValidationType.DCR);
        assertFalse(configs.isEmpty());
        assertTrue(configs.iterator().next().getIssuers().contains("Acme"));

        configs = ssaValidationConfigService.getByIssuer("jans-auth", SsaValidationType.SSA);
        assertFalse(configs.isEmpty());
        assertTrue(configs.iterator().next().getIssuers().contains("jans-auth"));
    }

    @Test
    public void prepareSsaJsonObject_whenScopesAreSet_shouldOverwriteScopesInResultObject() throws InvalidJwtException {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim("scope", Arrays.asList("scope1", "scope2"));

        SsaValidationConfig config = new SsaValidationConfig();
        config.setScopes(Arrays.asList("config_scope1", "config_scope2"));

        final JSONObject result = ssaValidationConfigService.prepareSsaJsonObject(jwtClaims, config);
        assertEquals(result.get("scope"), "config_scope1 config_scope2");
    }

    @Test
    public void prepareSsaJsonObject_whenClaimsAreSet_shouldOverwriteClaimsInResultObject() throws InvalidJwtException {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim("claims", Arrays.asList("claim1", "claim2"));

        SsaValidationConfig config = new SsaValidationConfig();
        config.setAllowedClaims(Arrays.asList("config_claim1", "config_claim2"));

        final JSONObject result = ssaValidationConfigService.prepareSsaJsonObject(jwtClaims, config);
        assertEquals(result.get("claims"), "config_claim1 config_claim2");
    }

    @Test
    public void validateSsaForBuiltIn_whenVerifiedSuccessfullyAndHasScopesAndClaimsSet_shouldOverwriteScopesAndClaims() throws InvalidJwtException, CryptoProviderException {
        // jwt with iss=jans-auth
        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqYW5zLWF1dGgiLCJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsInNjb3BlIjoic2NvcGUxIHNjb3BlMiIsImNsYWltcyI6ImNsYWltMSBjbGFpbTIiLCJpYXQiOjE2Njk4MDY3NjMsImV4cCI6MTY2OTgxMDM2M30.nR3LURANa5YAxOcLRdeFh0YjHbNA6roIUOhDfvhNeAw";

        when(cryptoProvider.verifySignature(any(), any(), any(), any(), any(), any())).thenReturn(true);

        final JSONObject result = ssaValidationConfigService.validateSsaForBuiltIn(Jwt.parseOrThrow(jwt));
        assertEquals(result.get("scope"), "my_read my_write");
        assertEquals(result.get("claims"), "test_exp test_iat");
    }

    @Test
    public void validateSsaForBuiltIn_whenVerifiedSuccessfullyAndHasNoScopesAndClaimsSet_shouldHaveOriginalJwtScopesAndClaims() throws InvalidJwtException, CryptoProviderException {
        // jwt with iss=empty
        String jwtString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlbXB0eSIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJhZG1pbiI6dHJ1ZSwic2NvcGUiOiJzY29wZTEgc2NvcGUyIiwiY2xhaW1zIjoiY2xhaW0xIGNsYWltMiIsImlhdCI6MTY2OTgwNjc2MywiZXhwIjoxNjY5ODEwMzYzfQ.db0WQh2lmHkNYCWT8tSW684hqWTPJDTElppy42XM_lc";

        when(cryptoProvider.verifySignature(any(), any(), any(), any(), any(), any())).thenReturn(true);

        final Jwt jwt = Jwt.parseOrThrow(jwtString);
        final Object scope = jwt.getClaims().getClaim("scope");
        final Object claims = jwt.getClaims().getClaim("claims");

        final JSONObject result = ssaValidationConfigService.validateSsaForBuiltIn(jwt);
        assertEquals(result.get("scope"), scope);
        assertEquals(result.get("claims"), claims);
    }
}
