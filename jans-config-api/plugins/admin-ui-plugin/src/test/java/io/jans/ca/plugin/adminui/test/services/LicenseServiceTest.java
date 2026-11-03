package io.jans.ca.plugin.adminui.test.services;

import io.jans.as.client.TokenRequest;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseConfig;
import io.jans.as.model.config.adminui.MainSettings;
import io.jans.as.model.config.adminui.OIDCClientSettings;
import io.jans.ca.plugin.adminui.model.auth.DCRResponse;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.service.license.LicenseDetailsService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.LicenseConfiguration;
import io.jans.orm.PersistenceEntryManager;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.slf4j.Logger;

import java.time.LocalDate;

@Listeners(MockitoTestNGListener.class)
public class LicenseServiceTest {
    @Mock
    private PersistenceEntryManager entryManager;
    @Mock
    private Logger log;
    @InjectMocks
    private LicenseDetailsService licenseDetailsService;
    @Mock
    private AUIConfigurationService auiConfigurationService;
    @Mock
    private LicenseConfiguration licenseConfiguration;

    private AdminConf adminConf;
    private LicenseConfig licenseConfig;
    private AUIConfiguration auiConfiguration;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);  // Initialize mocks

        adminConf = mock(AdminConf.class);
        licenseConfig = mock(LicenseConfig.class);
        auiConfiguration = mock(AUIConfiguration.class);
        lenient().when(entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN)).thenReturn(adminConf);
        lenient().when(adminConf.getMainSettings()).thenReturn(mock(MainSettings.class));
        lenient().when(adminConf.getMainSettings().getLicenseConfig()).thenReturn(licenseConfig);

        // The service also reads the in-memory license configuration; stub that path so
        // auiConfiguration / licenseConfiguration are never null during validation.
        lenient().when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        lenient().when(auiConfiguration.getLicenseConfiguration()).thenReturn(licenseConfiguration);
    }

    @Test
    void testValidateLicenseConfiguration_LicenseConfigAbsent() {
        when(adminConf.getMainSettings().getLicenseConfig()).thenReturn(null);
        GenericResponse response = licenseDetailsService.validateLicenseConfiguration();
        assertFalse(response.isSuccess());
        assertEquals(500, response.getResponseCode());
    }

    @Test
    void testValidateLicenseConfiguration_OIDCClientMissing() {
        lenient().when(licenseConfig.getLicenseKey()).thenReturn("valid-key");
        lenient().when(licenseConfig.getLicenseHardwareKey()).thenReturn("valid-hardware-key");
        when(licenseConfig.getOidcClient()).thenReturn(new OIDCClientSettings(null, null, null));

        GenericResponse response = licenseDetailsService.validateLicenseConfiguration();
        assertFalse(response.isSuccess());
        assertEquals(500, response.getResponseCode());
    }

    @Test
    void testValidateLicenseConfiguration_LicenseKeyAbsent() {

        lenient().when(licenseConfig.getSsa()).thenReturn("valid-ssa");
        lenient().when(licenseConfig.getScanLicenseApiHostname()).thenReturn("valid-scan-url");
        lenient().when(licenseConfig.getLicenseHardwareKey()).thenReturn("valid-hardware-key");
        when(licenseConfig.getOidcClient()).thenReturn(new OIDCClientSettings("test-host", "test-client-id", "test-client-secret"));

        GenericResponse response = licenseDetailsService.validateLicenseConfiguration();
        assertFalse(response.isSuccess());
        assertEquals(404, response.getResponseCode());
    }

    @Test
    void testValidateLicenseConfiguration_ValidConfig() {
        lenient().when(licenseConfig.getLicenseKey()).thenReturn("valid-key");
        lenient().when(licenseConfig.getSsa()).thenReturn("valid-ssa");
        lenient().when(licenseConfig.getScanLicenseApiHostname()).thenReturn("valid-scan-url");
        lenient().when(licenseConfig.getLicenseHardwareKey()).thenReturn("valid-hardware-key");
        when(licenseConfig.getOidcClient()).thenReturn(new OIDCClientSettings("test-host", "test-client-id", "test-client-secret"));
        when(licenseConfig.getIntervalForSyncLicenseDetailsInDays()).thenReturn(30L);

        // In-memory license details: not expired and recently synced, so no re-sync is triggered.
        lenient().when(licenseConfiguration.getLicenseValidUpto()).thenReturn(LocalDate.now().plusDays(30).toString());
        lenient().when(licenseConfiguration.getLicenseDetailsLastUpdatedOn()).thenReturn(LocalDate.now().minusDays(5).toString());

        GenericResponse response = licenseDetailsService.validateLicenseConfiguration();
        assertTrue(response.isSuccess());
        assertEquals(200, response.getResponseCode());
    }

    @Test
    public void testCheckLicense_ValidLicense() {
        // Mock the admin config and license config
        lenient().when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        lenient().when(auiConfiguration.getLicenseConfiguration()).thenReturn(licenseConfiguration);
        lenient().when(licenseConfiguration.getHardwareId()).thenReturn("hardware-key-123");
        lenient().when(licenseConfiguration.getLicenseKey()).thenReturn("valid-license-key");
        lenient().when(licenseConfiguration.getScanApiHostname()).thenReturn("https://scan.api.hostname");
        lenient().when(licenseConfiguration.getLicenseValidUpto()).thenReturn(LocalDate.now().plusDays(30).toString());
        lenient().when(licenseConfiguration.getLicenseDetailsLastUpdatedOn()).thenReturn(LocalDate.now().minusDays(5).toString());
        lenient().when(licenseConfiguration.getIntervalForSyncLicenseDetailsInDays()).thenReturn(10L);
        lenient().when(licenseConfiguration.getLicenseMAUThreshold()).thenReturn(500L);

        // Call the method under test
        GenericResponse response = licenseDetailsService.checkLicense();

        // Verify behavior and assert expected response
        assertNotNull(response);
        assertEquals(response.getResponseCode(), 200);
        assertEquals(response.getResponseMessage(), "Valid license present.");
        verify(entryManager, never()).find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
    }

    @Test
    public void testCheckLicense_MissingAdminConfOnSync() {
        when(licenseConfiguration.getHardwareId()).thenReturn("hardware-key-123");
        when(licenseConfiguration.getLicenseKey()).thenReturn("valid-license-key");
        when(licenseConfiguration.getScanApiHostname()).thenReturn("https://scan.api.hostname");
        when(licenseConfiguration.getLicenseValidUpto()).thenReturn("");
        when(entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN)).thenReturn(null);

        GenericResponse response = licenseDetailsService.checkLicense();

        assertFalse(response.isSuccess());
        assertEquals(response.getResponseCode(), 500);
        assertEquals(response.getResponseMessage(), ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
    }

    @Test
    public void testCheckLicense_DcrOnlyForInvalidClientAndDoesNotRepeatFailedRetry() {
        LicenseConfiguration runtimeLicenseConfiguration = new LicenseConfiguration();
        runtimeLicenseConfiguration.setHardwareId("hardware-key-123");
        runtimeLicenseConfiguration.setLicenseKey("valid-license-key");
        runtimeLicenseConfiguration.setScanApiHostname("https://scan.example");
        runtimeLicenseConfiguration.setScanAuthServerHostname("https://auth.example");
        runtimeLicenseConfiguration.setScanApiClientId("original-client");
        runtimeLicenseConfiguration.setScanApiClientSecret("original-secret");
        runtimeLicenseConfiguration.setLicenseValidUpto("");
        when(auiConfiguration.getLicenseConfiguration()).thenReturn(runtimeLicenseConfiguration);
        when(licenseConfig.getSsa()).thenReturn("ssa");
        MainSettings mainSettings = mock(MainSettings.class);
        when(adminConf.getMainSettings()).thenReturn(mainSettings);
        when(mainSettings.getLicenseConfig()).thenReturn(licenseConfig);

        LicenseDetailsService service = spy(licenseDetailsService);
        DCRResponse dcrResponse = mock(DCRResponse.class);
        when(dcrResponse.getOpHost()).thenReturn("https://auth.example");
        when(dcrResponse.getClientId()).thenReturn("replacement-client");
        when(dcrResponse.getClientSecret()).thenReturn("replacement-secret");
        when(dcrResponse.getHardwareId()).thenReturn("hardware-key-123");
        when(dcrResponse.getScanHostname()).thenReturn("https://scan.example");
        doReturn(dcrResponse).when(service).executeDCR("ssa");

        AtomicInteger tokenCalls = new AtomicInteger();
        doAnswer(invocation -> {
            int tokenCall = tokenCalls.incrementAndGet();
            Consumer<String> errorResponseConsumer = invocation.getArgument(3);
            if (errorResponseConsumer != null && tokenCall == 1) {
                errorResponseConsumer.accept("{\"error\":\"temporarily_unavailable\"}");
            } else if (errorResponseConsumer != null && (tokenCall == 2 || tokenCall == 4)) {
                errorResponseConsumer.accept("{\"error\":\"invalid_client\"}");
            }
            return null;
        }).when(service).getToken(any(TokenRequest.class), anyString(), isNull(), nullable(Consumer.class));

        service.checkLicense();
        assertEquals(runtimeLicenseConfiguration.getScanApiClientId(), "original-client");
        verify(service, never()).executeDCR("ssa");

        service.checkLicense();
        service.checkLicense();

        verify(service, times(1)).executeDCR("ssa");
        assertEquals(runtimeLicenseConfiguration.getScanApiClientId(), "replacement-client");
    }

    @Test
    public void testCheckLicense_RetriesPersistenceBeforeRepeatingDcr() {
        LicenseConfiguration runtimeLicenseConfiguration = new LicenseConfiguration();
        runtimeLicenseConfiguration.setHardwareId("hardware-key-123");
        runtimeLicenseConfiguration.setLicenseKey("valid-license-key");
        runtimeLicenseConfiguration.setScanApiHostname("https://scan.example");
        runtimeLicenseConfiguration.setScanAuthServerHostname("https://auth.example");
        runtimeLicenseConfiguration.setScanApiClientId("original-client");
        runtimeLicenseConfiguration.setScanApiClientSecret("original-secret");
        runtimeLicenseConfiguration.setLicenseValidUpto("");
        when(auiConfiguration.getLicenseConfiguration()).thenReturn(runtimeLicenseConfiguration);
        when(licenseConfig.getSsa()).thenReturn("ssa");
        MainSettings mainSettings = mock(MainSettings.class);
        when(adminConf.getMainSettings()).thenReturn(mainSettings);
        when(mainSettings.getLicenseConfig()).thenReturn(licenseConfig);

        LicenseDetailsService service = spy(licenseDetailsService);
        DCRResponse dcrResponse = mock(DCRResponse.class);
        when(dcrResponse.getOpHost()).thenReturn("https://auth.example");
        when(dcrResponse.getClientId()).thenReturn("replacement-client");
        when(dcrResponse.getClientSecret()).thenReturn("replacement-secret");
        when(dcrResponse.getHardwareId()).thenReturn("hardware-key-123");
        when(dcrResponse.getScanHostname()).thenReturn("https://scan.example");
        doReturn(dcrResponse).when(service).executeDCR("ssa");

        doThrow(new IllegalStateException("Temporary persistence failure"))
                .doNothing().when(entryManager).merge(adminConf);
        AtomicInteger tokenCalls = new AtomicInteger();
        io.jans.as.client.TokenResponse replacementTokenResponse = new io.jans.as.client.TokenResponse();
        replacementTokenResponse.setAccessToken("replacement-access-token");
        doAnswer(invocation -> {
            int tokenCall = tokenCalls.incrementAndGet();
            Consumer<String> errorResponseConsumer = invocation.getArgument(3);
            if (tokenCall <= 2) {
                errorResponseConsumer.accept("{\"error\":\"invalid_client\"}");
                return null;
            }
            return replacementTokenResponse;
        }).when(service).getToken(any(TokenRequest.class), anyString(), isNull(), nullable(Consumer.class));

        GenericResponse firstResponse = service.checkLicense();
        service.checkLicense();

        assertEquals(firstResponse.getResponseMessage(),
                ErrorResponse.ERROR_IN_SAVING_LICENSE_CLIENT.getDescription());
        ArgumentCaptor<TokenRequest> tokenRequestCaptor = ArgumentCaptor.forClass(TokenRequest.class);
        verify(service, times(3)).getToken(tokenRequestCaptor.capture(), anyString(), isNull(), nullable(Consumer.class));
        assertEquals(tokenRequestCaptor.getAllValues().get(2).getAuthUsername(), "replacement-client");

        verify(service, times(1)).executeDCR("ssa");
        verify(entryManager, times(2)).merge(adminConf);
        assertEquals(runtimeLicenseConfiguration.getScanApiClientId(), "replacement-client");
    }

    @Test
    public void testCheckLicense_ConcurrentInvalidClientRegistersOnce() throws Exception {
        LicenseConfiguration runtimeLicenseConfiguration = new LicenseConfiguration();
        runtimeLicenseConfiguration.setHardwareId("hardware-key-123");
        runtimeLicenseConfiguration.setLicenseKey("valid-license-key");
        runtimeLicenseConfiguration.setScanApiHostname("https://scan.example");
        runtimeLicenseConfiguration.setScanAuthServerHostname("https://auth.example");
        runtimeLicenseConfiguration.setScanApiClientId("original-client");
        runtimeLicenseConfiguration.setScanApiClientSecret("original-secret");
        runtimeLicenseConfiguration.setLicenseValidUpto("");
        when(auiConfiguration.getLicenseConfiguration()).thenReturn(runtimeLicenseConfiguration);
        when(licenseConfig.getSsa()).thenReturn("ssa");
        MainSettings mainSettings = mock(MainSettings.class);
        when(adminConf.getMainSettings()).thenReturn(mainSettings);
        when(mainSettings.getLicenseConfig()).thenReturn(licenseConfig);

        LicenseDetailsService service = spy(licenseDetailsService);
        DCRResponse dcrResponse = mock(DCRResponse.class);
        when(dcrResponse.getOpHost()).thenReturn("https://auth.example");
        when(dcrResponse.getClientId()).thenReturn("replacement-client");
        when(dcrResponse.getClientSecret()).thenReturn("replacement-secret");
        when(dcrResponse.getHardwareId()).thenReturn("hardware-key-123");
        when(dcrResponse.getScanHostname()).thenReturn("https://scan.example");
        doReturn(dcrResponse).when(service).executeDCR("ssa");

        AtomicInteger tokenCalls = new AtomicInteger();
        CountDownLatch initialTokenRequests = new CountDownLatch(2);
        io.jans.as.client.TokenResponse replacementTokenResponse = new io.jans.as.client.TokenResponse();
        replacementTokenResponse.setAccessToken("replacement-access-token");
        doAnswer(invocation -> {
            int tokenCall = tokenCalls.incrementAndGet();
            Consumer<String> errorResponseConsumer = invocation.getArgument(3);
            if (tokenCall <= 2) {
                initialTokenRequests.countDown();
                if (!initialTokenRequests.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent token requests did not both report invalid_client");
                }
                errorResponseConsumer.accept("{\"error\":\"invalid_client\"}");
                return null;
            }
            return replacementTokenResponse;
        }).when(service).getToken(any(TokenRequest.class), anyString(), isNull(), nullable(Consumer.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<GenericResponse> firstCheck = executor.submit(service::checkLicense);
            Future<GenericResponse> secondCheck = executor.submit(service::checkLicense);
            assertEquals(firstCheck.get(10, TimeUnit.SECONDS).getResponseCode(), 500);
            assertEquals(secondCheck.get(10, TimeUnit.SECONDS).getResponseCode(), 500);
        } finally {
            executor.shutdownNow();
        }

        verify(service, times(1)).executeDCR("ssa");
        verify(entryManager, times(1)).merge(adminConf);
        ArgumentCaptor<TokenRequest> tokenRequestCaptor = ArgumentCaptor.forClass(TokenRequest.class);
        verify(service, times(4)).getToken(tokenRequestCaptor.capture(), anyString(), isNull(), nullable(Consumer.class));
        assertEquals(tokenRequestCaptor.getAllValues().get(2).getAuthUsername(), "replacement-client");
        assertEquals(tokenRequestCaptor.getAllValues().get(3).getAuthUsername(), "replacement-client");
        java.lang.reflect.Field retryMarker = LicenseDetailsService.class.getDeclaredField("dcrRetryFailedClientId");
        retryMarker.setAccessible(true);
        assertNull(retryMarker.get(service));
        assertEquals(runtimeLicenseConfiguration.getScanApiClientId(), "replacement-client");
    }

    @Test
    public void testCheckLicense_MAUIsNull() {
        // Mock the admin config and license config
        lenient().when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        lenient().when(auiConfiguration.getLicenseConfiguration()).thenReturn(licenseConfiguration);
        lenient().when(licenseConfiguration.getHardwareId()).thenReturn("hardware-key-123");
        lenient().when(licenseConfiguration.getLicenseKey()).thenReturn("valid-license-key");//License key missing
        lenient().when(licenseConfiguration.getScanApiHostname()).thenReturn("https://scan.api.hostname");
        lenient().when(licenseConfiguration.getLicenseValidUpto()).thenReturn(LocalDate.now().plusDays(30).toString());
        lenient().when(licenseConfiguration.getLicenseDetailsLastUpdatedOn()).thenReturn(LocalDate.now().minusDays(5).toString());
        lenient().when(licenseConfiguration.getIntervalForSyncLicenseDetailsInDays()).thenReturn(10L);
        lenient().when(licenseConfiguration.getLicenseMAUThreshold()).thenReturn(null);

        // Call the method under test
        GenericResponse response = licenseDetailsService.checkLicense();

        // Verify behavior and assert expected response
        assertNotNull(response);
        assertEquals(response.getResponseCode(), 500);
        assertEquals(response.getResponseMessage(), ErrorResponse.MAU_IS_NULL.getDescription());
    }

    @Test
    public void testCheckLicense_NoLicenseKey() {
        // Mock missing license key
        lenient().when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        lenient().when(auiConfiguration.getLicenseConfiguration()).thenReturn(licenseConfiguration);
        lenient().when(licenseConfiguration.getHardwareId()).thenReturn("hardware-key-123");
        lenient().when(licenseConfiguration.getLicenseKey()).thenReturn("");//License key missing
        lenient().when(licenseConfiguration.getScanApiHostname()).thenReturn("https://scan.api.hostname");
        lenient().when(licenseConfiguration.getLicenseValidUpto()).thenReturn(LocalDate.now().plusDays(30).toString());
        lenient().when(licenseConfiguration.getLicenseDetailsLastUpdatedOn()).thenReturn(LocalDate.now().minusDays(5).toString());
        lenient().when(licenseConfiguration.getIntervalForSyncLicenseDetailsInDays()).thenReturn(10L);

        // Call the method under test
        GenericResponse response = licenseDetailsService.checkLicense();

        // Verify response for missing license key
        assertNotNull(response);
        assertEquals(response.getResponseCode(), 404);
        assertEquals(response.getResponseMessage(), ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
    }
}
