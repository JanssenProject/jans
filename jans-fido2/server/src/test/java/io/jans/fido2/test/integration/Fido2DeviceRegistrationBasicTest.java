package io.jans.fido2.test.integration;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.Fido2Service;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.attestation.PublicKeyCredentialCreationOptions;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Basic FIDO2 Device Registration Integration Test
 * 
 * This test verifies:
 * 1. FIDO2 service is available and properly configured
 * 2. Can generate attestation options for device registration
 * 3. Can find test user and device data
 * 4. Basic service connectivity
 */
public class Fido2DeviceRegistrationBasicTest {

    private static final Logger log = LoggerFactory.getLogger(Fido2DeviceRegistrationBasicTest.class);
    
    private static final String TEST_USER_ID = "test_fido_user";
    private static final String TEST_DEVICE_ID = "test-device-1";
    private static final String TEST_USER_INUM = "B1F3-AEAE-B798";

    @Inject
    private Fido2Service fido2Service;

    @Inject
    private AttestationService attestationService;

    @Inject
    private PersistenceEntryManager persistenceManager;

    private AppConfiguration appConfiguration;

    @BeforeClass
    public void setup() {
        log.info("Setting up FIDO2 Device Registration Basic Test");
        
        // Verify FIDO2 service is available
        assertNotNull(fido2Service, "FIDO2 service should be available");
        assertNotNull(attestationService, "Attestation service should be available");
        assertNotNull(persistenceManager, "Persistence manager should be available");
        
        // Load configuration
        appConfiguration = fido2Service.find();
        assertNotNull(appConfiguration, "FIDO2 configuration should be available");
        
        log.info("FIDO2 configuration loaded successfully");
        if (appConfiguration.getIssuer() != null) {
            log.info("Issuer: {}", appConfiguration.getIssuer());
        }
        if (appConfiguration.getBaseEndpoint() != null) {
            log.info("Base Endpoint: {}", appConfiguration.getBaseEndpoint());
        }
    }

    @Test(description = "Test FIDO2 service availability and basic configuration")
    public void testFido2ServiceAvailability() {
        log.info("Testing FIDO2 service availability");
        
        // Verify configuration is loaded
        assertNotNull(appConfiguration, "App configuration should not be null");
        assertNotNull(appConfiguration.getFido2Configuration(), "FIDO2 configuration should not be null");
        
        // Verify basic configuration values
        assertNotNull(appConfiguration.getIssuer(), "Issuer should not be null");
        assertNotNull(appConfiguration.getBaseEndpoint(), "Base endpoint should not be null");
        assertTrue(appConfiguration.getBaseEndpoint().contains("/jans-fido2/restv1"), 
                  "Base endpoint should contain FIDO2 REST path");
        
        log.info("FIDO2 service availability test passed");
    }

    @Test(description = "Test attestation options generation")
    public void testAttestationOptionsGeneration() {
        log.info("Testing attestation options generation");
        
        try {
            // Create attestation options request
            AttestationOptions attestationOptions = new AttestationOptions();
            attestationOptions.setUsername(TEST_USER_ID);
            attestationOptions.setDisplayName("Test User");
            
            // Generate attestation options
            PublicKeyCredentialCreationOptions options = attestationService.options(attestationOptions);
            
            // Verify options are generated
            assertNotNull(options, "Attestation options should not be null");
            assertNotNull(options.getChallenge(), "Challenge should not be null");
            assertNotNull(options.getRp(), "Relying party should not be null");
            assertNotNull(options.getUser(), "User should not be null");
            
            // Verify challenge format (should be base64 encoded)
            assertTrue(options.getChallenge().length() > 0, "Challenge should not be empty");
            
            log.info("Attestation options generation test passed");
            log.info("Challenge length: {}", options.getChallenge().length());
            if (options.getRp() != null && options.getRp().getName() != null) {
                log.info("Relying Party: {}", options.getRp().getName());
            }
            
        } catch (Exception e) {
            log.error("Failed to generate attestation options", e);
            fail("Attestation options generation failed: " + e.getMessage());
        }
    }

    @Test(description = "Test test user and device data availability")
    public void testTestDataAvailability() {
        log.info("Testing test data availability");
        
        try {
            // Search for test user
            String userDn = String.format("inum=%s,ou=people,o=jans", TEST_USER_INUM);
            Object user = persistenceManager.find(Object.class, userDn);
            assertNotNull(user, "Test user should exist in database");
            
            // Search for test device registration
            String deviceDn = String.format("jansId=%s,ou=fido2_register,inum=%s,ou=people,o=jans", 
                                          TEST_DEVICE_ID, TEST_USER_INUM);
            Fido2RegistrationEntry device = persistenceManager.find(Fido2RegistrationEntry.class, deviceDn);
            
            if (device != null) {
                // Verify device registration data
                assertNotNull(device.getId(), "Device ID should not be null");
                assertEquals(device.getId(), TEST_DEVICE_ID, "Device ID should match");
                assertNotNull(device.getDisplayName(), "Device display name should not be null");
                assertEquals(device.getRegistrationStatus(), Fido2RegistrationStatus.registered, 
                           "Device should be registered");
                
                log.info("Test device registration found and validated");
                log.info("Device ID: {}", device.getId());
                log.info("Display Name: {}", device.getDisplayName());
                if (device.getRegistrationStatus() != null) {
                    log.info("Status: {}", device.getRegistrationStatus().getValue());
                }
            } else {
                log.info("Test device registration not found (this is OK for initial test)");
            }
            
            log.info("Test data availability test passed");
            
        } catch (Exception e) {
            log.error("Failed to verify test data", e);
            fail("Test data verification failed: " + e.getMessage());
        }
    }

    @Test(description = "Test FIDO2 configuration validation")
    public void testFido2ConfigurationValidation() {
        log.info("Testing FIDO2 configuration validation");
        
        // Verify required configuration elements
        assertNotNull(appConfiguration.getFido2Configuration().getRequestedParties(), "Requested parties should be configured");
        assertTrue(appConfiguration.getFido2Configuration().getRequestedParties().size() > 0, "At least one RP should be configured");
        
        assertNotNull(appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms(), "Enabled algorithms should be configured");
        assertTrue(appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms().size() > 0, "At least one algorithm should be enabled");
        
        // Verify user auto enrollment is enabled for testing
        assertTrue(appConfiguration.getFido2Configuration().isUserAutoEnrollment(), "User auto enrollment should be enabled for testing");
        
        log.info("FIDO2 configuration validation passed");
        if (appConfiguration.getFido2Configuration().getRequestedParties() != null) {
            log.info("Requested Parties: {}", appConfiguration.getFido2Configuration().getRequestedParties().size());
        }
        if (appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms() != null) {
            log.info("Enabled Algorithms: {}", appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms().size());
        }
        log.info("User Auto Enrollment: {}", appConfiguration.getFido2Configuration().isUserAutoEnrollment());
    }
} 