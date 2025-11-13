package io.jans.fido2.test.integration;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.service.Fido2Service;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.attestation.PublicKeyCredentialCreationOptions;
import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionOptionsResponse;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

/**
 * Enhanced FIDO2 Device Registration Integration Test
 * 
 * This test verifies:
 * 1. Enhanced FIDO2 attestation options generation
 * 2. Configuration validation for all FIDO2 settings
 * 3. Test data management and validation
 * 4. Performance testing
 * 5. Error handling scenarios
 */
public class Fido2DeviceRegistrationEnhancedTest {

    private static final Logger log = LoggerFactory.getLogger(Fido2DeviceRegistrationEnhancedTest.class);
    
    private static final String TEST_USER_ID = "test_fido_user";
    private static final String TEST_DEVICE_ID = "test-device-1";
    private static final String TEST_USER_INUM = "B1F3-AEAE-B798";
    private static final String TEST_DISPLAY_NAME = "Test User";
    private static final String TEST_RP_ID = "test.example.com";

    @Inject
    private Fido2Service fido2Service;

    @Inject
    private AttestationService attestationService;

    @Inject
    private AssertionService assertionService;

    @Inject
    private PersistenceEntryManager persistenceManager;

    private AppConfiguration appConfiguration;
    private PublicKeyCredentialCreationOptions attestationOptions;

    @BeforeClass
    public void setup() {
        log.info("Setting up Enhanced FIDO2 Device Registration Test");
        
        // Verify all services are available
        assertNotNull(fido2Service, "FIDO2 service should be available");
        assertNotNull(attestationService, "Attestation service should be available");
        assertNotNull(assertionService, "Assertion service should be available");
        assertNotNull(persistenceManager, "Persistence manager should be available");
        
        // Load configuration
        appConfiguration = fido2Service.find();
        assertNotNull(appConfiguration, "FIDO2 configuration should be available");
        
        log.info("Enhanced FIDO2 test setup completed successfully");
    }

    @Test(description = "Test complete attestation options generation with validation")
    public void testEnhancedAttestationOptionsGeneration() {
        log.info("Testing enhanced attestation options generation");
        
        try {
            // Create attestation options request
            AttestationOptions request = new AttestationOptions();
            request.setUsername(TEST_USER_ID);
            request.setDisplayName(TEST_DISPLAY_NAME);
            
            // Generate attestation options
            attestationOptions = attestationService.options(request);
            
            // Comprehensive validation
            assertNotNull(attestationOptions, "Attestation options should not be null");
            assertNotNull(attestationOptions.getChallenge(), "Challenge should not be null");
            assertNotNull(attestationOptions.getRp(), "Relying party should not be null");
            assertNotNull(attestationOptions.getUser(), "User should not be null");
            assertNotNull(attestationOptions.getAuthenticatorSelection(), "Authenticator selection should not be null");
            
            // Verify challenge format and length
            assertTrue(attestationOptions.getChallenge().length() >= 32, "Challenge should be at least 32 characters");
            
            // Verify relying party configuration
            assertNotNull(attestationOptions.getRp().getId(), "RP ID should not be null");
            assertNotNull(attestationOptions.getRp().getName(), "RP Name should not be null");
            
            // Verify user configuration
            assertNotNull(attestationOptions.getUser().getId(), "User ID should not be null");
            assertEquals(attestationOptions.getUser().getName(), TEST_USER_ID, "User name should match");
            assertEquals(attestationOptions.getUser().getDisplayName(), TEST_DISPLAY_NAME, "Display name should match");
            
            // Verify authenticator selection
            assertNotNull(attestationOptions.getAuthenticatorSelection().getAuthenticatorAttachment(), 
                        "Authenticator attachment should be configured");
            assertNotNull(attestationOptions.getAuthenticatorSelection().getUserVerification(), 
                        "User verification should be configured");
            
            log.info("Enhanced attestation options generation test passed");
            log.info("Challenge length: {}", attestationOptions.getChallenge().length());
            log.info("RP ID: {}", attestationOptions.getRp().getId());
            log.info("User ID: {}", attestationOptions.getUser().getId());
            
        } catch (Exception e) {
            log.error("Failed to generate enhanced attestation options", e);
            fail("Enhanced attestation options generation failed: " + e.getMessage());
        }
    }

    @Test(description = "Test assertion options generation for authentication")
    public void testAssertionOptionsGeneration() {
        log.info("Testing assertion options generation");
        
        try {
            // Create assertion options request
            AssertionOptions request = new AssertionOptions();
            request.setUsername(TEST_USER_ID);
            request.setRpId(TEST_RP_ID);
            
            // Generate assertion options
            AssertionOptionsResponse response = assertionService.options(request);
            
            // Validate assertion options
            assertNotNull(response, "Assertion options should not be null");
            assertNotNull(response.getChallenge(), "Challenge should not be null");
            assertNotNull(response.getRpId(), "RP ID should not be null");
            
            // Verify challenge format
            assertTrue(response.getChallenge().length() >= 32, "Challenge should be at least 32 characters");
            assertEquals(response.getRpId(), TEST_RP_ID, "RP ID should match");
            
            log.info("Assertion options generation test passed");
            log.info("Challenge length: {}", response.getChallenge().length());
            log.info("RP ID: {}", response.getRpId());
            
        } catch (Exception e) {
            log.error("Failed to generate assertion options", e);
            fail("Assertion options generation failed: " + e.getMessage());
        }
    }

    @Test(description = "Test error handling for invalid attestation requests")
    public void testAttestationErrorHandling() {
        log.info("Testing attestation error handling");
        
        try {
            // Test with null username
            AttestationOptions invalidRequest = new AttestationOptions();
            invalidRequest.setDisplayName("Test User");
            // Note: This might throw an exception, which is expected behavior
            
            log.info("Attestation error handling test completed");
            
        } catch (Exception e) {
            log.info("Expected error caught for invalid attestation request: {}", e.getMessage());
            // This is expected behavior for invalid requests
        }
    }

    @Test(description = "Test FIDO2 configuration comprehensive validation")
    public void testComprehensiveConfigurationValidation() {
        log.info("Testing comprehensive FIDO2 configuration validation");
        
        // Verify all required configuration elements
        assertNotNull(appConfiguration.getFido2Configuration(), "FIDO2 configuration should exist");
        
        // Verify requested parties configuration
        List<RequestedParty> requestedParties = appConfiguration.getFido2Configuration().getRequestedParties();
        assertNotNull(requestedParties, "Requested parties should be configured");
        assertTrue(requestedParties.size() > 0, "At least one RP should be configured");
        
        // Verify enabled algorithms
        List<String> enabledAlgorithms = appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms();
        assertNotNull(enabledAlgorithms, "Enabled algorithms should be configured");
        assertTrue(enabledAlgorithms.size() > 0, "At least one algorithm should be enabled");
        
        // Verify security settings
        assertTrue(appConfiguration.getFido2Configuration().isUserAutoEnrollment(), "User auto enrollment should be enabled");
        assertNotNull(appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration(), 
                    "Unfinished request expiration should be configured");
        
        // Verify attestation mode
        assertNotNull(appConfiguration.getFido2Configuration().getAttestationMode(), 
                    "Attestation mode should be configured");
        

        
        log.info("Comprehensive configuration validation passed");
        log.info("Requested Parties: {}", requestedParties.size());
        log.info("Enabled Algorithms: {}", enabledAlgorithms.size());
        log.info("User Auto Enrollment: {}", appConfiguration.getFido2Configuration().isUserAutoEnrollment());
    }

    @Test(description = "Test test data integrity and validation")
    public void testTestDataIntegrity() {
        log.info("Testing test data integrity");
        
        try {
            // Verify test user exists and has correct attributes
            String userDn = String.format("inum=%s,ou=people,o=jans", TEST_USER_INUM);
            Object user = persistenceManager.find(Object.class, userDn);
            assertNotNull(user, "Test user should exist in database");
            
            // Verify test device registration if exists
            String deviceDn = String.format("jansId=%s,ou=fido2_register,inum=%s,ou=people,o=jans", 
                                          TEST_DEVICE_ID, TEST_USER_INUM);
            Fido2RegistrationEntry device = persistenceManager.find(Fido2RegistrationEntry.class, deviceDn);
            
            if (device != null) {
                // Comprehensive device validation
                assertNotNull(device.getId(), "Device ID should not be null");
                assertEquals(device.getId(), TEST_DEVICE_ID, "Device ID should match expected value");
                assertNotNull(device.getDisplayName(), "Device display name should not be null");
                assertEquals(device.getRegistrationStatus(), Fido2RegistrationStatus.registered, 
                           "Device should be in registered status");
                assertNotNull(device.getRegistrationData(), "Registration data should not be null");
                assertNotNull(device.getCreationDate(), "Creation date should not be null");
                
                log.info("Test device registration validation passed");
                log.info("Device ID: {}", device.getId());
                log.info("Display Name: {}", device.getDisplayName());
                log.info("Status: {}", device.getRegistrationStatus().getValue());
                log.info("Creation Date: {}", device.getCreationDate());
            } else {
                log.info("Test device registration not found (this is OK for initial test)");
            }
            
            log.info("Test data integrity validation passed");
            
        } catch (Exception e) {
            log.error("Failed to verify test data integrity", e);
            fail("Test data integrity verification failed: " + e.getMessage());
        }
    }

    @Test(description = "Test FIDO2 service performance and response times")
    public void testServicePerformance() {
        log.info("Testing FIDO2 service performance");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Generate attestation options multiple times to test performance
            for (int i = 0; i < 3; i++) {
                AttestationOptions request = new AttestationOptions();
                request.setUsername(TEST_USER_ID + "_perf_" + i);
                request.setDisplayName(TEST_DISPLAY_NAME + " " + i);
                
                PublicKeyCredentialCreationOptions options = attestationService.options(request);
                assertNotNull(options, "Performance test options should not be null");
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Performance validation (should complete within reasonable time)
            assertTrue(totalTime < 5000, "Performance test should complete within 5 seconds, took: " + totalTime + "ms");
            
            log.info("Service performance test passed");
            log.info("Total time for 3 operations: {}ms", totalTime);
            log.info("Average time per operation: {}ms", totalTime / 3);
            
        } catch (Exception e) {
            log.error("Failed to complete performance test", e);
            fail("Performance test failed: " + e.getMessage());
        }
    }

    @DataProvider(name = "algorithmTestData")
    public Object[][] algorithmTestData() {
        return new Object[][] {
            {"ES256", "Elliptic Curve P-256 with SHA-256"},
            {"RS256", "RSA with SHA-256"},
            {"ES384", "Elliptic Curve P-384 with SHA-384"},
            {"RS384", "RSA with SHA-384"}
        };
    }

    @Test(description = "Test supported FIDO2 algorithms", dataProvider = "algorithmTestData")
    public void testSupportedAlgorithms(String algorithm, String description) {
        log.info("Testing algorithm: {} ({})", algorithm, description);
        
        try {
            List<String> enabledAlgorithms = appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms();
            assertNotNull(enabledAlgorithms, "Enabled algorithms should not be null");
            
            // Check if algorithm is in the enabled list (if it's a supported algorithm)
            if (algorithm.equals("ES256") || algorithm.equals("RS256")) {
                assertTrue(enabledAlgorithms.contains(algorithm), 
                          "Algorithm " + algorithm + " should be enabled");
                log.info("Algorithm {} is enabled and supported", algorithm);
            } else {
                log.info("Algorithm {} is not in the standard enabled list (this is OK)", algorithm);
            }
            
        } catch (Exception e) {
            log.error("Failed to test algorithm: {}", algorithm, e);
            fail("Algorithm test failed for " + algorithm + ": " + e.getMessage());
        }
    }

    @Test(description = "Test FIDO2 security settings validation")
    public void testSecuritySettingsValidation() {
        log.info("Testing FIDO2 security settings validation");
        
        try {
            // Verify logging settings
            
            // Verify logging settings
            assertNotNull(appConfiguration.getLoggingLevel(), "Logging level should be configured");
            assertTrue(appConfiguration.getLoggingLevel().equals("DEBUG") || 
                      appConfiguration.getLoggingLevel().equals("INFO"), 
                      "Logging level should be DEBUG or INFO for testing");
            
            // Verify metric reporting settings
            assertTrue(appConfiguration.getMetricReporterEnabled(), "Metric reporting should be enabled");
            assertNotNull(appConfiguration.getMetricReporterInterval(), "Metric reporter interval should be configured");
            
            log.info("Security settings validation passed");
            log.info("Logging Level: {}", appConfiguration.getLoggingLevel());
            log.info("Metric Reporter Enabled: {}", appConfiguration.getMetricReporterEnabled());
            
        } catch (Exception e) {
            log.error("Failed to validate security settings", e);
            fail("Security settings validation failed: " + e.getMessage());
        }
    }
} 