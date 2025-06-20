package io.jans.as.server.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.external.ExternalApplicationSessionService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.stat.StatService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class SessionIdServiceTest {

    @InjectMocks
    private SessionIdService sessionIdService;

    @Mock
    private Logger log;

    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

    @Mock
    private ExternalApplicationSessionService externalApplicationSessionService;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private RequestParameterService requestParameterService;

    @Mock
    private UserService userService;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private CookieService cookieService;

    @Mock
    private Identity identity;

    @Mock
    private LocalCacheService localCacheService;

    @Mock
    private CacheService cacheService;

    @Mock
    private StatService statService;

    @Mock
    private AttributeService attributeService;

    @Test
    public void isAgamaInSessionAndRequest_forAgama_shouldReturnTrue() {
        assertTrue(SessionIdService.isAgamaInSessionAndRequest("agama", Lists.newArrayList("agama_io.jans.agamaLab.main")));
        assertTrue(SessionIdService.isAgamaInSessionAndRequest("agama", Lists.newArrayList("agama")));
    }

    @Test
    public void isAgamaInSessionAndRequest_forBasic_shouldReturnFalse() {
        assertFalse(SessionIdService.isAgamaInSessionAndRequest("agama", Lists.newArrayList("basic")));
        assertFalse(SessionIdService.isAgamaInSessionAndRequest("basic", Lists.newArrayList("agama_io.jans.agamaLab.main")));
    }

    @Test
    public void hasAllScopes_whenSessionIsNull_shouldReturnFalse() {
        assertFalse(sessionIdService.hasAllScopes((SessionId) null, null));
    }

    @Test
    public void hasAllScopes_whenSessionHasNoScopeAttribute_shouldReturnFalse() {
        assertFalse(sessionIdService.hasAllScopes(new SessionId(), null));
    }

    @Test
    public void hasAllScopes_whenSessionHasNotAllScopes_shouldReturnFalse() {
        final SessionId sessionId = new SessionId();
        sessionId.getSessionAttributes().put("scope", "openid profile");

        assertFalse(sessionIdService.hasAllScopes(sessionId, Sets.newHashSet("email")));
        assertFalse(sessionIdService.hasAllScopes(sessionId, Sets.newHashSet("test")));
    }

    @Test
    public void hasAllScopes_whenSessionHasAllScopes_shouldReturnTrue() {
        final SessionId sessionId = new SessionId();
        sessionId.getSessionAttributes().put("scope", "openid profile");

        assertTrue(sessionIdService.hasAllScopes(sessionId, Sets.newHashSet("openid", "profile")));
        assertTrue(sessionIdService.hasAllScopes(sessionId, Sets.newHashSet("profile")));
        assertTrue(sessionIdService.hasAllScopes(sessionId, Sets.newHashSet("openid")));
    }

}
