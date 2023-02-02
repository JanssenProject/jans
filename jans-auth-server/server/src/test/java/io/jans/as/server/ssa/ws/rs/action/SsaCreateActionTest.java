package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.client.ssa.create.SsaCreateRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.common.service.AttributeService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.ssa.SsaConfiguration;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.util.DateUtil;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ModifySsaResponseService;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import io.jans.as.server.ssa.ws.rs.SsaContextBuilder;
import io.jans.as.server.ssa.ws.rs.SsaJsonService;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Listeners(MockitoTestNGListener.class)
public class SsaCreateActionTest {

    @InjectMocks
    private SsaCreateAction ssaCreateAction;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private InumService inumService;

    @Mock
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Mock
    private SsaService ssaService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private Logger log;

    @Mock
    private SsaContextBuilder ssaContextBuilder;

    @Mock
    private ModifySsaResponseService modifySsaResponseService;

    @Mock
    private SsaJsonService ssaJsonService;

    private Ssa ssa;
    private JSONObject requestJson;

    @BeforeMethod
    public void setUp() {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        ssa = new Ssa();
        ssa.setOrgId("org-id-test");
        ssa.setExpirationDate(calendar.getTime());
        ssa.setDescription("test description");
        ssa.getAttributes().setSoftwareId("gluu-scan-api");
        ssa.getAttributes().setSoftwareRoles(Collections.singletonList("password"));
        ssa.getAttributes().setGrantTypes(Collections.singletonList("client_credentials"));
        ssa.getAttributes().setOneTimeUse(true);
        ssa.getAttributes().setRotateSsa(true);

        requestJson = new JSONObject();
        requestJson.put(ORG_ID.getName(), ssa.getOrgId());
        requestJson.put(EXPIRATION.getName(), DateUtil.dateToUnixEpoch(ssa.getExpirationDate()));
        requestJson.put(DESCRIPTION.getName(), ssa.getDescription());
        requestJson.put(SOFTWARE_ID.getName(), ssa.getAttributes().getSoftwareId());
        requestJson.put(SOFTWARE_ROLES.getName(), ssa.getAttributes().getSoftwareRoles());
        requestJson.put(GRANT_TYPES.getName(), ssa.getAttributes().getGrantTypes());
        requestJson.put(ONE_TIME_USE.getName(), ssa.getAttributes().getOneTimeUse());
        requestJson.put(ROTATE_SSA.getName(), ssa.getAttributes().getRotateSsa());
    }

    @Test
    public void create_request_valid() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);
        when(inumService.generateDefaultId()).thenReturn(UUID.randomUUID().toString());
        Client client = new Client();
        client.setDn("inum=0000,ou=clients,o=jans");
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(client);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        ModifySsaResponseContext context = mock(ModifySsaResponseContext.class);
        when(ssaContextBuilder.buildModifySsaResponseContext(any(), any())).thenReturn(context);
        when(modifySsaResponseService.buildCreateProcessor(any())).thenReturn(jsonWebResponse -> null);
        when(context.toExecutionContext()).thenReturn(executionContext);
        when(ssaService.generateJwt(any(), any())).thenReturn(mock(Jwt.class));
        when(ssaJsonService.getJSONObject(anyString())).thenReturn(mock(JSONObject.class));
        when(ssaJsonService.jsonObjectToString(any())).thenReturn("{\"ssa\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\"}");
        when(appConfiguration.getSsaConfiguration()).thenReturn(new SsaConfiguration());

        Response response = ssaCreateAction.create(requestJson.toString(), mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");
        assertNotNull(response.getEntity(), "response entity is null");
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(log).debug(anyString(), any(SsaCreateRequest.class));
        verify(staticConfiguration).getBaseDn();
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(), anyString());
        verify(ssaService).persist(any());
        verify(log).info(anyString(), any(Ssa.class));
        verify(ssaContextBuilder).buildModifySsaResponseContext(any(), any());
        verify(modifySsaResponseService).buildCreateProcessor(any());
        verify(context).toExecutionContext();
        verify(ssaService).generateJwt(any(), any());
        verify(ssaJsonService).getJSONObject(anyString());
        verify(ssaJsonService).jsonObjectToString(any());

        verifyNoInteractions(attributeService);
        verify(appConfiguration).getSsaConfiguration();

        ArgumentCaptor<Ssa> ssaCaptor = ArgumentCaptor.forClass(Ssa.class);
        verify(ssaService).persist(ssaCaptor.capture());
        Ssa ssaAux = ssaCaptor.getValue();
        assertNotNull(ssaAux, "ssa is null");
        assertNotNull(ssaAux.getOrgId(), "ssa org_id is null");
        assertEquals(ssaAux.getOrgId(), ssa.getOrgId());
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
        assertNotNull(ssaAux.getState(), "ssa state is null");
        assertEquals(ssaAux.getState(), SsaState.ACTIVE);
    }

    @Test
    public void create_disabledSsaComponent_forbiddenResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("Component is disabled on server.")
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
        doThrow(error).when(errorResponseFactory).validateFeatureEnabled(any());

        try {
            ssaCreateAction.create(requestJson.toString(), mock(HttpServletRequest.class));
        } catch (WebApplicationException e) {
            assertNotNull(e, "Exception is null");
            assertNotNull(e.getResponse(), "Exception Response is null");
        }
        verifyNoInteractions(log, staticConfiguration, inumService, ssaRestWebServiceValidator, ssaService, ssaContextBuilder,
                modifySsaResponseService, ssaJsonService, appConfiguration, attributeService);
    }

    @Test
    public void create_invalidClientAndIsErrorEnabledFalse_badRequestResponse() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);
        when(inumService.generateDefaultId()).thenReturn(UUID.randomUUID().toString());
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid client")
                        .build());
        doThrow(error).when(ssaRestWebServiceValidator).getClientFromSession();
        when(log.isErrorEnabled()).thenReturn(Boolean.FALSE);

        try {
            ssaCreateAction.create(requestJson.toString(), mock(HttpServletRequest.class));
        } catch (WebApplicationException e) {
            assertNotNull(e, "Exception is null");
            assertNotNull(e.getResponse(), "Exception Response is null");
        }
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(log).isErrorEnabled();
        verify(log, never()).error(anyString(), any(WebApplicationException.class));
        verify(ssaRestWebServiceValidator, never()).checkScopesPolicy(any(), anyString());
        verify(ssaService, never()).persist(any(Ssa.class));
        verifyNoInteractions(ssaService, ssaContextBuilder, modifySsaResponseService, ssaJsonService, appConfiguration, attributeService);
    }

    @Test
    public void create_invalidClientAndIsErrorEnabledTrue_badRequestResponse() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setSsa("ou=ssa,o=jans");
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);
        when(inumService.generateDefaultId()).thenReturn(UUID.randomUUID().toString());
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid client")
                        .build());
        doThrow(error).when(ssaRestWebServiceValidator).getClientFromSession();
        when(log.isErrorEnabled()).thenReturn(Boolean.TRUE);

        try {
            ssaCreateAction.create(requestJson.toString(), mock(HttpServletRequest.class));
        } catch (WebApplicationException e) {
            assertNotNull(e, "Exception is null");
            assertNotNull(e.getResponse(), "Exception Response is null");
        }
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(log).isErrorEnabled();
        verify(log).error(anyString(), any(WebApplicationException.class));
        verify(ssaRestWebServiceValidator, never()).checkScopesPolicy(any(), anyString());
        verify(ssaService, never()).persist(any(Ssa.class));
        verifyNoInteractions(ssaService, ssaContextBuilder, modifySsaResponseService, ssaJsonService, appConfiguration, attributeService);
    }

    @Test
    public void create_invalidClient_internalServerResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Unknown error")
                        .build());
        when(errorResponseFactory.createWebApplicationException(any(Response.Status.class), any(SsaErrorResponseType.class), anyString())).thenThrow(error);

        try {
            ssaCreateAction.create(requestJson.toString(), mock(HttpServletRequest.class));
        } catch (WebApplicationException e) {
            assertNotNull(e, "Exception is null");
        }
        verify(staticConfiguration).getBaseDn();
        verify(log).debug(anyString(), any(SsaCreateRequest.class));
        verify(log).error(eq(null), any(NullPointerException.class));
        verify(ssaRestWebServiceValidator, never()).checkScopesPolicy(any(), anyString());
        verify(ssaService, never()).persist(any(Ssa.class));
        verifyNoInteractions(ssaService, ssaContextBuilder, modifySsaResponseService, ssaJsonService, appConfiguration, attributeService);
    }
}