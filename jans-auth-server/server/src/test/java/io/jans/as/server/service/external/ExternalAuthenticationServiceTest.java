package io.jans.as.server.service.external;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.external.internal.InternalDefaultPersonAuthenticationType;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.ldap.GluuLdapConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import jakarta.inject.Named;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class ExternalAuthenticationServiceTest {

    @InjectMocks
    @Spy
    private ExternalAuthenticationService externalAuthenticationService;

    @Mock
    @Named(ApplicationFactory.PERSISTENCE_AUTH_CONFIG_NAME)
    private List<GluuLdapConfiguration> ldapAuthConfigs;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private InternalDefaultPersonAuthenticationType authenticationType;

    private final static String BASIC = "basic";
    private final static String BASIC_LOCK = "basic_lock";
    private final static String CASA = "casa";
    private final static String OTP = "otp";

    @Test
    public void determineCustomScriptConfiguration_acrValuesNull_customScriptBasicInteractive() {
        final List<String> acrValuesList = null;
        Mockito.doReturn(new ArrayList<>()).when(externalAuthenticationService).getAuthModesByAcrValues(acrValuesList);
        Mockito.doReturn(true).when(appConfiguration).getUseHighestLevelScriptIfAcrScriptNotFound();
        externalAuthenticationService.setDefaultExternalAuthenticators(createsetDefaultExternalAuthenticatorsWithInteractive());
        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNotNull(customScriptConfiguration);
        assertNotNull(customScriptConfiguration.getCustomScript());
        assertEquals(customScriptConfiguration.getCustomScript().getName(), BASIC);
    }

    @Test
    public void determineCustomScriptConfiguration_acrValuesEmpty_customScriptNotNull() {
        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());

        final List<String> acrValuesList = new ArrayList<>();
        Mockito.doReturn(new ArrayList<>()).when(externalAuthenticationService).getAuthModesByAcrValues(acrValuesList);
        Mockito.doReturn(true).when(appConfiguration).getUseHighestLevelScriptIfAcrScriptNotFound();

        externalAuthenticationService.setDefaultExternalAuthenticators(createsetDefaultExternalAuthenticatorsWithInteractive());
        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNotNull(customScriptConfiguration);
        assertNotNull(customScriptConfiguration.getCustomScript());
        assertEquals(customScriptConfiguration.getCustomScript().getName(), BASIC);
    }

    @Test
    public void determineCustomScriptConfiguration_usageTypeInteractive_customScriptNotNull() {
        final List<String> acrValuesList = Arrays.asList(BASIC);

        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());
        externalAuthenticationService.setDefaultExternalAuthenticators(createsetDefaultExternalAuthenticatorsWithInteractive());

        Mockito.doReturn(Arrays.asList(AuthenticationScriptUsageType.INTERACTIVE.toString())).when(externalAuthenticationService).getAuthModesByAcrValues(anyList());
        Mockito.doReturn(true).when(appConfiguration).getUseHighestLevelScriptIfAcrScriptNotFound();

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.SERVICE, acrValuesList);

        assertNotNull(customScriptConfiguration);
        assertNotNull(customScriptConfiguration.getCustomScript());
        assertEquals(customScriptConfiguration.getCustomScript().getName(), BASIC_LOCK);
    }

    @Test
    public void determineCustomScriptConfiguration_getCustomScriptConfigurationsMapByUsageTypeInteractive_customScriptNotNull() {
        final String acrValue = CASA;
        final List<String> acrValuesList = Arrays.asList(acrValue);

        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());
        externalAuthenticationService.setDefaultExternalAuthenticators(createsetDefaultExternalAuthenticatorsWithInteractive());

        Mockito.doReturn(Arrays.asList(CASA)).when(externalAuthenticationService).getAuthModesByAcrValues(anyList());

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNotNull(customScriptConfiguration);
        assertNotNull(customScriptConfiguration.getCustomScript());
        assertEquals(customScriptConfiguration.getCustomScript().getName(), CASA);
    }

    @Test
    public void determineCustomScriptConfiguration_customScriptConfigurationsMapByUsageTypeCasa_customScriptNull() {
        final String acrValue = CASA;
        final List<String> acrValuesList = Arrays.asList(acrValue);

        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());
        externalAuthenticationService.setDefaultExternalAuthenticators(createsetDefaultExternalAuthenticatorsWithInteractive());

        Mockito.doReturn(Arrays.asList(BASIC, CASA, BASIC_LOCK)).when(externalAuthenticationService).getAuthModesByAcrValues(anyList());

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNotNull(customScriptConfiguration);
        assertNotNull(customScriptConfiguration.getCustomScript());
        assertEquals(customScriptConfiguration.getCustomScript().getName(), CASA);
    }

    @Test
    public void determineCustomScriptConfiguration_witAcrValuesNull_customScriptConfNull() {
        final List<String> acrValuesList = new ArrayList<>();
        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());
        Mockito.doReturn(acrValuesList).when(externalAuthenticationService).getAuthModesByAcrValues(anyList());

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNull(customScriptConfiguration);
    }

    @Test
    public void determineCustomScriptConfiguration_withAuthModesEmpty_scriptBasic() {
        final List<String> acrValuesList = new ArrayList<>();
        externalAuthenticationService.setDefaultExternalAuthenticators(createsetDefaultExternalAuthenticatorsWithInteractive());
        Mockito.doReturn(acrValuesList).when(externalAuthenticationService).getAuthModesByAcrValues(anyList());
        Mockito.doReturn(true).when(appConfiguration).getUseHighestLevelScriptIfAcrScriptNotFound();

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNotNull(customScriptConfiguration);
        assertNotNull(customScriptConfiguration.getCustomScript());
        assertEquals(customScriptConfiguration.getCustomScript().getName(), "basic");
    }

    @Test
    public void determineCustomScriptConfiguration_withAuthModesEmptyIfAcrsNull_false() {
        final List<String> acrValuesList = new ArrayList<>();
        externalAuthenticationService.setCustomScriptConfigurationsMapByUsageType(createCustomScriptConfigurationsMapByUsageTypeWithInteractive());
        Mockito.doReturn(acrValuesList).when(externalAuthenticationService).getAuthModesByAcrValues(anyList());
        Mockito.doReturn(false).when(appConfiguration).getUseHighestLevelScriptIfAcrScriptNotFound();

        final CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

        assertNull(customScriptConfiguration);
    }

    private CustomScript createCustomScript(String name) {
        final CustomScript script = new CustomScript();
        script.setName(name);
        script.setLevel(100);
        script.setInternal(false);
        return script;
    }

    private Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> createCustomScriptConfigurationsMapByUsageTypeWithInteractive() {
        final Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> customScriptConfigurationsMapByUsageType = new HashMap<>();
        final CustomScript casaCustomScript = createCustomScript(CASA);
        final CustomScript otpCustomScript = createCustomScript(OTP);

        final CustomScriptConfiguration scriptInteractive = new CustomScriptConfiguration(casaCustomScript, authenticationType, new HashMap<>());
        final List<CustomScriptConfiguration> customList = Arrays.asList(scriptInteractive);

        final CustomScriptConfiguration scriptService = new CustomScriptConfiguration(otpCustomScript, authenticationType, new HashMap<>());
        final List<CustomScriptConfiguration> customListService = Arrays.asList(scriptService);

        customScriptConfigurationsMapByUsageType.put(AuthenticationScriptUsageType.INTERACTIVE, customList);
        customScriptConfigurationsMapByUsageType.put(AuthenticationScriptUsageType.SERVICE, customListService);

        return customScriptConfigurationsMapByUsageType;
    }

    private Map<AuthenticationScriptUsageType, CustomScriptConfiguration> createsetDefaultExternalAuthenticatorsWithInteractive() {
        final Map<AuthenticationScriptUsageType, CustomScriptConfiguration> defaultExternalAuthenticators = new HashMap<>();
        final CustomScript basic = createCustomScript(BASIC);
        final CustomScript basicLock = createCustomScript(BASIC_LOCK);

        final CustomScriptConfiguration scriptBasic = new CustomScriptConfiguration(basic, authenticationType, new HashMap<>());
        final CustomScriptConfiguration scriptBasicLock = new CustomScriptConfiguration(basicLock, authenticationType, new HashMap<>());

        defaultExternalAuthenticators.put(AuthenticationScriptUsageType.INTERACTIVE, scriptBasic);
        defaultExternalAuthenticators.put(AuthenticationScriptUsageType.SERVICE, scriptBasicLock);

        return defaultExternalAuthenticators;
    }
}
