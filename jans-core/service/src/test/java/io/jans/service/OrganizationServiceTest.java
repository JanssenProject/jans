/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import io.jans.model.ApplicationType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Unit tests for OrganizationService
 * 
 * @author Janssen Project
 */
public class OrganizationServiceTest {

    private TestOrganizationService organizationService;

    @BeforeClass
    public void setUp() {
        organizationService = new TestOrganizationService();
    }

    @Test
    public void testGetDnForOrganization_withNullBaseDn_shouldReturnDefault() {
        String result = organizationService.getDnForOrganization(null);
        
        assertNotNull(result);
        assertEquals(result, "o=jans");
    }

    @Test
    public void testGetDnForOrganization_withEmptyBaseDn_shouldReturnEmpty() {
        String result = organizationService.getDnForOrganization("");
        
        assertNotNull(result);
        assertEquals(result, "");
    }

    @Test
    public void testGetDnForOrganization_withValidBaseDn_shouldReturnSame() {
        String customDn = "o=custom";
        String result = organizationService.getDnForOrganization(customDn);
        
        assertEquals(result, customDn);
    }

    @Test
    public void testGetDnForOrganization_withComplexDn_shouldReturnSame() {
        String complexDn = "ou=people,o=jans";
        String result = organizationService.getDnForOrganization(complexDn);
        
        assertEquals(result, complexDn);
    }

    @Test
    public void testGetBaseDn_shouldReturnDefaultBaseDn() {
        String baseDn = organizationService.getBaseDn();
        
        assertNotNull(baseDn);
        assertEquals(baseDn, "o=jans");
    }

    @Test
    public void testGetBaseDn_multipleCalls_shouldReturnConsistentValue() {
        String baseDn1 = organizationService.getBaseDn();
        String baseDn2 = organizationService.getBaseDn();
        
        assertEquals(baseDn1, baseDn2);
    }

    @Test
    public void testGetApplicationType_shouldReturnConfiguredType() {
        ApplicationType appType = organizationService.getApplicationType();
        
        assertNotNull(appType);
        assertEquals(appType, ApplicationType.OX_AUTH);
    }

    @Test
    public void testConstants_oneMinuteInSeconds() {
        assertEquals(OrganizationService.ONE_MINUTE_IN_SECONDS, 60);
    }

    // Test implementation of abstract OrganizationService
    private static class TestOrganizationService extends OrganizationService {
        private static final long serialVersionUID = 1L;

        @Override
        public ApplicationType getApplicationType() {
            return ApplicationType.OX_AUTH;
        }
    }
}