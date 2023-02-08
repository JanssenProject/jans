package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.ssa.SsaConfiguration;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import jakarta.ws.rs.WebApplicationException;
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

import java.util.*;

import static io.jans.as.model.ssa.SsaRequestParam.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaServiceTest {

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

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    private Ssa ssa;

    @BeforeMethod
    public void setUp() {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        ssa = new Ssa();
        ssa.setId(UUID.randomUUID().toString());
        ssa.setOrgId("test-org-id-1000");
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
        String orgId = null;
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
        String orgId = null;
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
        String orgId = "org-id-test-1";
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
        String orgId = null;
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
        String orgId = null;
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
    public void generateJwt_executionContextWithPostProcessorNull_jwtValid() throws Exception {
        SsaConfiguration ssaConfiguration = new SsaConfiguration();
        String issuer = "https://test.jans.io";
        when(appConfiguration.getSsaConfiguration()).thenReturn(ssaConfiguration);
        when(appConfiguration.getIssuer()).thenReturn(issuer);
        ExecutionContext executionContext = mock(ExecutionContext.class);

        Jwt jwt = ssaService.generateJwt(ssa, executionContext);
        assertSsaJwt(ssaConfiguration.getSsaSigningAlg(), issuer, ssa, jwt);
        verify(executionContext).getPostProcessor();
        verifyNoMoreInteractions(executionContext);
    }

    @Test
    public void generateJwt_executionContextWithPostProcessor_jwtValid() throws Exception {
        SsaConfiguration ssaConfiguration = new SsaConfiguration();
        String issuer = "https://test.jans.io";
        when(appConfiguration.getSsaConfiguration()).thenReturn(ssaConfiguration);
        when(appConfiguration.getIssuer()).thenReturn(issuer);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getPostProcessor()).thenReturn(jsonWebResponse -> null);

        Jwt jwt = ssaService.generateJwt(ssa, executionContext);
        assertSsaJwt(ssaConfiguration.getSsaSigningAlg(), issuer, ssa, jwt);
        verify(executionContext, times(2)).getPostProcessor();
    }

    @Test
    public void generateJwt_ssa_jwtValid() throws Exception {
        SsaConfiguration ssaConfiguration = new SsaConfiguration();
        String issuer = "https://test.jans.io";
        when(appConfiguration.getSsaConfiguration()).thenReturn(ssaConfiguration);
        when(appConfiguration.getIssuer()).thenReturn(issuer);

        Jwt jwt = ssaService.generateJwt(ssa);
        assertSsaJwt(ssaConfiguration.getSsaSigningAlg(), issuer, ssa, jwt);
        verify(cryptoProvider).sign(any(), any(), eq(null), any());
        verifyNoInteractions(log);
    }

    @Test
    public void generateJwt_signatureAlgorithmNull_invalidSignature() {
        SsaConfiguration ssaConfiguration = new SsaConfiguration();
        ssaConfiguration.setSsaSigningAlg("WRONG-SIGNING-ALG");
        when(appConfiguration.getSsaConfiguration()).thenReturn(ssaConfiguration);
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"invalid_signature\",\"description\":\"No algorithm found to sign the JWT.\"}")
                        .build());
        when(errorResponseFactory.createWebApplicationException(any(), any(), anyString())).thenThrow(error);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaService.generateJwt(ssa));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 400);
        assertNotNull(ex.getResponse().getEntity());

        JSONObject jsonObject = new JSONObject(ex.getResponse().getEntity().toString());
        assertTrue(jsonObject.has("error"));
        assertEquals(jsonObject.get("error"), "invalid_signature");
        assertTrue(jsonObject.has("description"));

        verify(log).error(anyString(), anyString());
        verifyNoMoreInteractions(log);
        verifyNoInteractions(cryptoProvider, webKeysConfiguration);
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

    private static void assertSsaJwt(String ssaSigningAlg, String issuer, Ssa ssa, Jwt jwt) {
        assertNotNull(jwt, "The jwt is null");

        JwtHeader jwtHeader = jwt.getHeader();
        assertNotNull(jwtHeader.getSignatureAlgorithm().getJwsAlgorithm(), "The alg in jwt is null");
        assertEquals(jwtHeader.getSignatureAlgorithm().getJwsAlgorithm().toString(), ssaSigningAlg);
        assertNotNull(jwtHeader.getType(), "The type in jwt is null");
        assertEquals(jwtHeader.getType().toString(), "jwt");

        JwtClaims jwtClaims = jwt.getClaims();
        assertNotNull(jwtClaims.getClaim(ORG_ID.getName()), "The org_id in jwt is null");
        assertEquals(jwtClaims.getClaim(ORG_ID.getName()), ssa.getOrgId());
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