package io.jans.as.server.ssa.ws.rs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.ssa.SsaConfiguration;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.*;

import static io.jans.as.model.ssa.SsaRequestParam.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaServiceTest {

    private final String senderJwkJson = "{\n" +
            "    \"kty\": \"RSA\",\n" +
            "    \"d\": \"iSx-zxihgOITpEhz6WwGiiCZjxx597wqblhSYgFWa_bL9esLY3FT_Kq9sdvGPiI8QmObRxPZuTi4n3BVKYUWcfjVz3swq7VmESxnJJZE-vMI9NTaZ-CT2b4I-c3qwAsejhWagJf899I3MRtPOnyxMimyOw4_5YYvXjBkXkCMfCsbj5TBR3RbtMrUYzDMXsVT1EJ_7H76DPBFJx5JptsEAA17VMtqwvWhRutnPyQOftDGPxD-1aGgpteKOUCv7Lx-mFX-zV6nnPB8vmgTgaMqCbCFKSZI567p714gzWBkwnNdRHleX8wos8yZAGbdwGqqUz5x3iKKdn3c7U9TTU7DAQ\",\n" +
            "    \"e\": \"AQAB\",\n" +
            "    \"use\": \"sig\",\n" +
            "    \"kid\": \"1\",\n" +
            "    \"alg\": \"RS256\",\n" +
            "    \"n\": \"i6tdK2fREwykTUU-qkYkiSHgg9B31-8EjVCbH0iyrewY9s7_WYPT7I3argjcmiDkufnVfGGW0FadtO3br-Qgk_N2e9LqGMtjUoGMZKFS3fJhqjnLYDi_E5l2FYU_ilw4EXPsZJY0CaM7BxjwUBoCjopYrgvtdxA9G6gpGoAH4LopAkgX-gkawVLpB4NpLvA09FLF2OlYZL7aaybvM2Lz_IXEPa-LSOwLum80Et-_A1-YMx_Z767Iwl1pGTpgZ87jrDD1vEdMdiLcWFG3UIYAAIxtg6X23cvQVLMaXKpyV0USDCWRJrZYxEDgZngbDRj3Sd2-LnixPkMWAfo_D9lBVQ\"\n" +
            "}";

    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private Logger log;

    @InjectMocks
    private SsaService ssaService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @Mock
    private StaticConfiguration staticConfiguration;

    private Ssa ssa;

    @BeforeMethod
    public void setUp() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        cryptoProvider = new AbstractCryptoProvider() {

            @Override
            public JSONObject generateKey(Algorithm algorithm, Long expirationTime) throws CryptoProviderException {
                return null;
            }

            @Override
            public JSONObject generateKey(Algorithm algorithm, Long expirationTime, int keyLength) throws CryptoProviderException {
                return null;
            }

            @Override
            public String sign(String signingInput, String keyId, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
                try {
                    RSAPrivateKey privateKey = ((RSAKey) JWK.parse(senderJwkJson)).toRSAPrivateKey();
                    Signature signature = Signature.getInstance(signatureAlgorithm.getAlgorithm(), "BC");
                    signature.initSign(privateKey);
                    signature.update(signingInput.getBytes());

                    return Base64Util.base64urlencode(signature.sign());
                } catch (JOSEException | ParseException | NoSuchAlgorithmException | NoSuchProviderException |
                         InvalidKeyException | SignatureException e) {
                    throw new CryptoProviderException(e);
                }
            }

            @Override
            public boolean verifySignature(String signingInput, String encodedSignature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
                return false;
            }

            @Override
            public boolean deleteKey(String keyId) throws CryptoProviderException {
                return false;
            }

            @Override
            public boolean containsKey(String keyId) {
                return false;
            }

            @Override
            public PrivateKey getPrivateKey(String keyId) throws CryptoProviderException {
                return null;
            }

            @Override
            public PublicKey getPublicKey(String alias) throws CryptoProviderException {
                return null;
            }
        };

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        ssa = new Ssa();
        ssa.setId(UUID.randomUUID().toString());
        ssa.setOrgId("1");
        ssa.setExpirationDate(calendar.getTime());
        ssa.setDescription("Test description");
        ssa.getAttributes().setSoftwareId("scan-api-test");
        ssa.getAttributes().setSoftwareRoles(Collections.singletonList("password"));
        ssa.getAttributes().setGrantTypes(Collections.singletonList("client_credentials"));
        ssa.getAttributes().setOneTimeUse(true);
        ssa.getAttributes().setRotateSsa(true);
    }

    @Test
    public void persist_ssa_valid() {
        ssaService.persist(ssa);

        verify(persistenceEntryManager).persist(any(Ssa.class));
        verifyNoInteractions(log);

        ArgumentCaptor<Ssa> ssaArgumentCaptor = ArgumentCaptor.forClass(Ssa.class);
        verify(persistenceEntryManager).persist(ssaArgumentCaptor.capture());
        assertSsaWithAux(ssa, ssaArgumentCaptor.getValue());
    }

    @Test
    public void merge_ssa_valid() {
        ssaService.merge(ssa);

        verify(persistenceEntryManager).merge(any(Ssa.class));
        verifyNoInteractions(log);

        ArgumentCaptor<Ssa> ssaArgumentCaptor = ArgumentCaptor.forClass(Ssa.class);
        verify(persistenceEntryManager).merge(ssaArgumentCaptor.capture());
        assertSsaWithAux(ssa, ssaArgumentCaptor.getValue());
    }

    @Test
    public void findSsaByJti_jtiValid_ssaValid() {
        String jti = "my-jti";
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);
        when(persistenceEntryManager.find(any(), anyString())).thenReturn(ssa);

        Ssa ssaAux = ssaService.findSsaByJti(jti);
        assertNotNull(ssaAux, "ssa is null");
        verifyNoMoreInteractions(persistenceEntryManager);
    }

    @Test
    public void findSsaByJti_jtiNotFound_ssaNull() {
        String jti = "my-jti";
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);
        EntryPersistenceException error = new EntryPersistenceException(" Failed to lookup entry by key");
        when(persistenceEntryManager.find(any(), anyString())).thenThrow(error);

        Ssa ssaAux = ssaService.findSsaByJti(jti);
        assertNull(ssaAux, "ssa is not null");
        verifyNoMoreInteractions(persistenceEntryManager);
    }

    @Test
    public void getSsaList_withDeveloperScope_valid() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

        String jti = null;
        Long orgId = null;
        SsaState status = null;
        String clientId = "test-client";
        String[] scopes = new String[]{SsaScopeType.SSA_DEVELOPER.getValue()};
        List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, status, clientId, scopes);
        assertNotNull(ssaList);
        verify(log).trace(eq("Filter with AND created: " + String.format("[(creatorId=%s)]", clientId)));
        verify(persistenceEntryManager).findEntries(any(), any(), any());
        verifyNoMoreInteractions(log);
    }

    @Test
    public void getSsaList_withJti_valid() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

        String jti = "test-jti";
        Long orgId = null;
        SsaState status = null;
        String clientId = "test-client";
        String[] scopes = new String[]{};
        List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, status, clientId, scopes);
        assertNotNull(ssaList);
        verify(log).trace(eq("Filter with AND created: " + String.format("[(inum=%s)]", jti)));
        verify(persistenceEntryManager).findEntries(any(), any(), any());
        verifyNoMoreInteractions(log);
    }

    @Test
    public void getSsaList_withOrgId_valid() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

        String jti = null;
        Long orgId = 1000L;
        SsaState status = null;
        String clientId = "test-client";
        String[] scopes = new String[]{};
        List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, status, clientId, scopes);
        assertNotNull(ssaList);
        verify(log).trace(eq("Filter with AND created: " + String.format("[(o=%s)]", orgId)));
        verify(persistenceEntryManager).findEntries(any(), any(), any());
        verifyNoMoreInteractions(log);
    }

    @Test
    public void getSsaList_withStatus_valid() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

        String jti = null;
        Long orgId = null;
        SsaState status = SsaState.ACTIVE;
        String clientId = "test-client";
        String[] scopes = new String[]{};
        List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, status, clientId, scopes);
        assertNotNull(ssaList);
        verify(log).trace(eq("Filter with AND created: " + String.format("[(jansState=%s)]", status)));
        verify(persistenceEntryManager).findEntries(any(), any(), any());
        verifyNoMoreInteractions(log);
    }

    @Test
    public void getSsaList_withNullParam_valid() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

        String jti = null;
        Long orgId = null;
        SsaState status = null;
        String clientId = null;
        String[] scopes = new String[]{};
        List<Ssa> ssaList = ssaService.getSsaList(jti, orgId, status, clientId, scopes);
        assertNotNull(ssaList);
        assertTrue(ssaList.isEmpty());
        verify(persistenceEntryManager).findEntries(any(), any(), any());
        verifyNoInteractions(log);
    }

    @Test
    public void generateJwt_executionContextWithPostProcessorNull_jwtValid() {
        JSONWebKey jsonWebKey = JSONWebKey.fromJSONObject(new JSONObject(senderJwkJson));
        WebKeysConfiguration webKeysConfiguration = new WebKeysConfiguration();
        webKeysConfiguration.setKeys(Collections.singletonList(jsonWebKey));

        SsaConfiguration ssaConfiguration = new SsaConfiguration();
        String issuer = "https://jans.io";
        when(appConfiguration.getSsaConfiguration()).thenReturn(ssaConfiguration);
        when(appConfiguration.getIssuer()).thenReturn(issuer);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        Jwt jwt = ssaService.generateJwt(ssa, executionContext, webKeysConfiguration, cryptoProvider);
        assertSsaJwt(jsonWebKey, ssaConfiguration.getSsaSigningAlg(), issuer, ssa, jwt);
        verify(executionContext).getPostProcessor();
    }

    @Test
    public void generateJwt_executionContextWithPostProcessor_jwtValid() {
        JSONWebKey jsonWebKey = JSONWebKey.fromJSONObject(new JSONObject(senderJwkJson));
        WebKeysConfiguration webKeysConfiguration = new WebKeysConfiguration();
        webKeysConfiguration.setKeys(Collections.singletonList(jsonWebKey));

        SsaConfiguration ssaConfiguration = new SsaConfiguration();
        String issuer = "https://jans.io";
        when(appConfiguration.getSsaConfiguration()).thenReturn(ssaConfiguration);
        when(appConfiguration.getIssuer()).thenReturn(issuer);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getPostProcessor()).thenReturn(jsonWebResponse -> null);

        Jwt jwt = ssaService.generateJwt(ssa, executionContext, webKeysConfiguration, cryptoProvider);
        assertSsaJwt(jsonWebKey, ssaConfiguration.getSsaSigningAlg(), issuer, ssa, jwt);
        verify(executionContext, times(2)).getPostProcessor();
    }

    @Test
    public void generateJwt_exceptionWithIsErrorEnabledFalse_runtimeException() {
        when(log.isErrorEnabled()).thenReturn(false);
        try {
            ssaService.generateJwt(ssa, mock(ExecutionContext.class), mock(WebKeysConfiguration.class), cryptoProvider);
        } catch (Exception e) {
            assertNotNull(e, "Exception is null");
        }
        verify(log).isErrorEnabled();
        verifyNoMoreInteractions(log);
    }

    @Test
    public void generateJwt_exceptionWithIsErrorEnabledTrue_runtimeException() {
        when(log.isErrorEnabled()).thenReturn(true);
        try {
            ssaService.generateJwt(ssa, mock(ExecutionContext.class), mock(WebKeysConfiguration.class), cryptoProvider);
        } catch (Exception e) {
            assertNotNull(e, "Exception is null");
        }
        verify(log).isErrorEnabled();
        verify(log).error(anyString(), any(Throwable.class));
    }

    @Test
    public void createNotAcceptableResponse_valid_response() {
        Response response = ssaService.createNotAcceptableResponse().build();
        assertNotNull(response, "Response is null");
        assertEquals(response.getStatus(), HttpStatus.SC_NOT_ACCEPTABLE);
    }

    @Test
    public void createUnprocessableEntityResponse_valid_response() {
        Response response = ssaService.createUnprocessableEntityResponse().build();
        assertNotNull(response, "Response is null");
        assertEquals(response.getStatus(), HttpStatus.SC_UNPROCESSABLE_ENTITY);
    }

    private static void assertSsaJwt(JSONWebKey jsonWebKey, String ssaSigningAlg, String issuer, Ssa ssa, Jwt jwt) {
        assertNotNull(jwt, "The jwt is null");

        JwtHeader jwtHeader = jwt.getHeader();
        assertNotNull(jwtHeader.getSignatureAlgorithm().getJwsAlgorithm(), "The alg in jwt is null");
        assertEquals(jwtHeader.getSignatureAlgorithm().getJwsAlgorithm().toString(), ssaSigningAlg);
        assertNotNull(jwtHeader.getKeyId(), "The kid in jwt is null");
        assertEquals(jwtHeader.getKeyId(), jsonWebKey.getKid());
        assertNotNull(jwtHeader.getType(), "The type in jwt is null");
        assertEquals(jwtHeader.getType().toString(), "jwt");

        JwtClaims jwtClaims = jwt.getClaims();
        assertNotNull(jwtClaims.getClaim(ORG_ID.getName()), "The org_id in jwt is null");
        assertEquals(jwtClaims.getClaim(ORG_ID.getName()), Long.parseLong(ssa.getOrgId()));
        assertNotNull(jwtClaims.getClaim(SOFTWARE_ID.getName()), "The software_id in jwt is null");
        assertEquals(jwtClaims.getClaim(SOFTWARE_ID.getName()), ssa.getAttributes().getSoftwareId());
        assertNotNull(jwtClaims.getClaim(SOFTWARE_ROLES.getName()), "The software_roles in jwt is null");
        assertEquals(jwtClaims.getClaim(SOFTWARE_ROLES.getName()), ssa.getAttributes().getSoftwareRoles());
        assertNotNull(jwtClaims.getClaim(GRANT_TYPES.getName()), "The grant_types in jwt is null");
        assertEquals(jwtClaims.getClaim(GRANT_TYPES.getName()), ssa.getAttributes().getGrantTypes());

        assertNotNull(jwtClaims.getClaim(JTI.getName()), "The jti in jwt is null");
        assertEquals(jwtClaims.getClaim(JTI.getName()), ssa.getId());
        assertNotNull(jwtClaims.getClaim(ISS.getName()), "The iss in jwt is null");
        assertEquals(jwtClaims.getClaim(ISS.getName()), issuer);
        assertNotNull(jwtClaims.getClaim(IAT.getName()), "The iat in jwt is null");
        assertEquals(jwtClaims.getClaim(IAT.getName()), ssa.getCreationDate());
        assertNotNull(jwtClaims.getClaim(EXP.getName()), "The exp in jwt is null");
        assertEquals(jwtClaims.getClaim(EXP.getName()), ssa.getExpirationDate());
    }

    private static void assertSsaWithAux(Ssa ssa, Ssa ssaAux) {
        assertNotNull(ssaAux, "ssa is null");
        assertNotNull(ssaAux.getId(), "ssa id is null");
        assertEquals(ssaAux.getId(), ssa.getId());
        assertNotNull(ssaAux.getOrgId(), "ssa org_id is null");
        assertEquals(ssaAux.getOrgId(), ssa.getOrgId());
        assertNotNull(ssaAux.getExpirationDate(), "ssa expiration is null");
        assertEquals(ssaAux.getExpirationDate(), ssa.getExpirationDate());
        assertNotNull(ssaAux.getDescription(), "ssa description is null");
        assertEquals(ssaAux.getDescription(), ssa.getDescription());
        assertNotNull(ssaAux.getAttributes().getSoftwareId(), "ssa software_id is null");
        assertEquals(ssaAux.getAttributes().getSoftwareId(), ssa.getAttributes().getSoftwareId());
        assertNotNull(ssaAux.getAttributes().getSoftwareRoles(), "ssa software_roles is null");
        assertEquals(ssaAux.getAttributes().getSoftwareRoles(), ssa.getAttributes().getSoftwareRoles());
        assertNotNull(ssaAux.getAttributes().getGrantTypes(), "ssa grant_types is null");
        assertEquals(ssaAux.getAttributes().getGrantTypes(), ssa.getAttributes().getGrantTypes());
        assertNotNull(ssaAux.getAttributes().getOneTimeUse(), "ssa one_time_use is null");
        assertEquals(ssaAux.getAttributes().getOneTimeUse(), ssa.getAttributes().getOneTimeUse());
        assertNotNull(ssaAux.getAttributes().getRotateSsa(), "ssa rotate_ssa is null");
        assertEquals(ssaAux.getAttributes().getRotateSsa(), ssa.getAttributes().getRotateSsa());
    }
}