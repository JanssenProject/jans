package io.jans.ca.plugin.adminui.test.services;

import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseConfig;
import io.jans.as.model.config.adminui.MainSettings;
import io.jans.as.model.config.adminui.OIDCClientSettings;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.service.license.LicenseDetailsService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

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

    private AdminConf adminConf;
    private LicenseConfig licenseConfig;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks

        adminConf = mock(AdminConf.class);
        licenseConfig = mock(LicenseConfig.class);
        when(entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN)).thenReturn(adminConf);
        when(adminConf.getMainSettings()).thenReturn(mock(MainSettings.class));
        when(adminConf.getMainSettings().getLicenseConfig()).thenReturn(licenseConfig);
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
        when(licenseConfig.getLicenseDetailsLastUpdatedOn()).thenReturn("2024-01-01");
        when(licenseConfig.getIntervalForSyncLicenseDetailsInDays()).thenReturn(30L);

        GenericResponse response = licenseDetailsService.validateLicenseConfiguration();
        assertTrue(response.isSuccess());
        assertEquals(200, response.getResponseCode());
    }

    @Test
    public void testCheckLicense_ValidLicense() {
        // Mock the admin config and license config

        when(licenseConfig.getLicenseHardwareKey()).thenReturn("hardware-key-123");
        when(licenseConfig.getLicenseKey()).thenReturn("valid-license-key");
        when(licenseConfig.getScanLicenseApiHostname()).thenReturn("https://scan.api.hostname");
        when(licenseConfig.getLicenseValidUpto()).thenReturn(LocalDate.now().plusDays(30).toString());
        when(licenseConfig.getLicenseDetailsLastUpdatedOn()).thenReturn(LocalDate.now().minusDays(5).toString());
        when(licenseConfig.getIntervalForSyncLicenseDetailsInDays()).thenReturn(10L);

        // Call the method under test
        GenericResponse response = licenseDetailsService.checkLicense();

        // Verify behavior and assert expected response
        assertNotNull(response);
        assertEquals(response.getResponseCode(), 200);
        assertEquals(response.getResponseMessage(), "Valid license present.");
    }

    @Test
    public void testCheckLicense_NoLicenseKey() {
        // Mock missing license key
        lenient().when(licenseConfig.getLicenseHardwareKey()).thenReturn("hardware-key-123");
        lenient().when(licenseConfig.getLicenseKey()).thenReturn("");//License key missing
        lenient().when(licenseConfig.getScanLicenseApiHostname()).thenReturn("https://scan.api.hostname");
        lenient().when(licenseConfig.getLicenseValidUpto()).thenReturn(LocalDate.now().plusDays(30).toString());
        lenient().when(licenseConfig.getLicenseDetailsLastUpdatedOn()).thenReturn(LocalDate.now().minusDays(5).toString());
        lenient().when(licenseConfig.getIntervalForSyncLicenseDetailsInDays()).thenReturn(10L);

        // Call the method under test
        GenericResponse response = licenseDetailsService.checkLicense();

        // Verify response for missing license key
        assertNotNull(response);
        assertEquals(response.getResponseCode(), 404);
        assertEquals(response.getResponseMessage(), ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
    }
}
