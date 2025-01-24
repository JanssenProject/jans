package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.service.AuthorizeService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalConsentGatheringService;
import io.jans.jsf2.service.FacesService;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authz.DummyConsentGatheringType;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class ConsentGathererServiceTest {

    @InjectMocks
    private ConsentGathererService consentGathererService;

    @Mock
    private Logger log;

    @Mock
    private ExternalConsentGatheringService external;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private FacesService facesService;

    @Mock
    private LanguageBean languageBean;

    @Mock
    private ConsentGatheringSessionService sessionService;

    @Mock
    private UserService userService;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private ClientService clientService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private ScopeChecker scopeChecker;

    @Test
    public void findConsentScriptByAcr_whenConfigurationMappingIsNotSet_shouldReturnNull() {
        final CustomScriptConfiguration agama = consentGathererService.findConsentScriptByAcr(Lists.newArrayList("agama"));
        assertNull(agama);
    }

    @Test
    public void findConsentScriptByAcr_whenConfigurationMappingIsPresent_shouldReturnScript() {
        final CustomScript agamaConsentScript = new CustomScript();
        agamaConsentScript.setName("consentScript");
        CustomScriptConfiguration agamaConsent = new CustomScriptConfiguration(agamaConsentScript, new DummyConsentGatheringType(), new HashMap<>());

        String acr = "agama";
        Map<String, String> configuration = new HashMap<>();
        configuration.put(acr, "consentScript");

        when(appConfiguration.getAcrToConsentScriptNameMapping()).thenReturn(configuration);
        when(external.getCustomScriptConfigurationByName(anyString())).thenReturn(agamaConsent);

        final CustomScriptConfiguration agama = consentGathererService.findConsentScriptByAcr(Lists.newArrayList(acr));
        assertNotNull(agama);
        assertEquals("consentScript", agama.getName());
    }
}
