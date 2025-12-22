package io.jans.as.server.service.external;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.DummyUpdateTokenType;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

@Listeners(MockitoTestNGListener.class)
public class ExternalUpdateTokenServiceTest {

    @Spy
    @InjectMocks
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void getScripts_whenNoScripts_shouldReturnEmptyList() {
        assertTrue(externalUpdateTokenService.getScripts(new ExternalUpdateTokenContext()).isEmpty());
    }

    @Test
    public void getScripts_whenScriptsPresentAndRunAllIsTrue_shouldReturnNonEmptyList() {
        List<CustomScriptConfiguration> scripts = new ArrayList<>();
        scripts.add(new CustomScriptConfiguration(new CustomScript("", "", ""), new DummyUpdateTokenType(), new HashMap<>()));

        when(appConfiguration.getRunAllUpdateTokenScripts()).thenReturn(true);
        when(externalUpdateTokenService.getCustomScriptConfigurations()).thenReturn(scripts);

        assertFalse(externalUpdateTokenService.getScripts(new ExternalUpdateTokenContext()).isEmpty());
    }

    @Test
    public void getScripts_whenScriptsPresentAndRunAllIsFalse_shouldReturnEmptyList() {
        List<CustomScriptConfiguration> scripts = new ArrayList<>();
        scripts.add(new CustomScriptConfiguration(new CustomScript("", "", ""), new DummyUpdateTokenType(), new HashMap<>()));

        when(appConfiguration.getRunAllUpdateTokenScripts()).thenReturn(false);
        when(externalUpdateTokenService.getCustomScriptConfigurations()).thenReturn(scripts);

        assertTrue(externalUpdateTokenService.getScripts(new ExternalUpdateTokenContext()).isEmpty());
    }

    @Test
    public void getScripts_whenScriptsPresentAndRunAllIsFalseAndClientHasNoScripts_shouldReturnEmptyList() {
        List<CustomScriptConfiguration> scripts = new ArrayList<>();
        scripts.add(new CustomScriptConfiguration(new CustomScript("", "", ""), new DummyUpdateTokenType(), new HashMap<>()));

        when(appConfiguration.getRunAllUpdateTokenScripts()).thenReturn(false);
        when(externalUpdateTokenService.getCustomScriptConfigurations()).thenReturn(scripts);

        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext();
        context.setClient(new Client());

        assertTrue(externalUpdateTokenService.getScripts(context).isEmpty());
    }

    @Test
    public void getScripts_whenScriptsPresentAndRunAllIsFalseAndClientHasScripts_shouldReturnNonEmptyList() {
        List<CustomScriptConfiguration> scripts = new ArrayList<>();
        scripts.add(new CustomScriptConfiguration(new CustomScript("dummy", "", ""), new DummyUpdateTokenType(), new HashMap<>()));

        when(appConfiguration.getRunAllUpdateTokenScripts()).thenReturn(false);
        when(externalUpdateTokenService.getCustomScriptConfigurations()).thenReturn(scripts);
        when(externalUpdateTokenService.getCustomScriptConfigurationsByDns(anyList())).thenReturn(scripts);

        Client client = new Client();
        client.getAttributes().setUpdateTokenScriptDns(List.of("dummy"));

        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext();
        context.setClient(client);

        assertFalse(externalUpdateTokenService.getScripts(context).isEmpty());
    }
}
