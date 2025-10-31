/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import io.jans.as.common.service.common.external.ExternalIdGeneratorService;
import io.jans.as.model.common.IdType;
import io.jans.util.StringHelper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for InumService
 * 
 * @author Janssen Project
 */
public class InumServiceTest {

    @Mock
    private Logger log;

    @Mock
    private ExternalIdGeneratorService externalIdGenerationService;

    @InjectMocks
    private InumService inumService;

    @BeforeClass
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateId_whenExternalServiceDisabled_shouldGenerateDefaultId() {
        when(externalIdGenerationService.isEnabled()).thenReturn(false);
        
        String result = inumService.generateId(IdType.CLIENTS);
        
        assertNotNull(result);
        assertTrue(StringHelper.isNotEmpty(result));
        // UUID format: 8-4-4-4-12 characters
        assertTrue(result.length() >= 36);
    }

    @Test
    public void testGenerateId_whenExternalServiceEnabledAndReturnsId_shouldUseExternalId() {
        String externalId = "external-generated-id";
        when(externalIdGenerationService.isEnabled()).thenReturn(true);
        when(externalIdGenerationService.executeExternalDefaultGenerateIdMethod(
            eq("jans-auth"), eq(IdType.CLIENTS), eq("")))
            .thenReturn(externalId);
        
        String result = inumService.generateId(IdType.CLIENTS);
        
        assertEquals(result, externalId);
        verify(externalIdGenerationService).executeExternalDefaultGenerateIdMethod(
            "jans-auth", IdType.CLIENTS, "");
    }

    @Test
    public void testGenerateId_whenExternalServiceEnabledButReturnsEmpty_shouldGenerateDefaultId() {
        when(externalIdGenerationService.isEnabled()).thenReturn(true);
        when(externalIdGenerationService.executeExternalDefaultGenerateIdMethod(
            eq("jans-auth"), eq(IdType.CLIENTS), eq("")))
            .thenReturn("");
        
        String result = inumService.generateId(IdType.CLIENTS);
        
        assertNotNull(result);
        assertTrue(StringHelper.isNotEmpty(result));
    }

    @Test
    public void testGenerateId_whenExternalServiceEnabledButReturnsNull_shouldGenerateDefaultId() {
        when(externalIdGenerationService.isEnabled()).thenReturn(true);
        when(externalIdGenerationService.executeExternalDefaultGenerateIdMethod(
            anyString(), anyString(), anyString()))
            .thenReturn(null);
        
        String result = inumService.generateId(IdType.PEOPLE);
        
        assertNotNull(result);
        assertTrue(StringHelper.isNotEmpty(result));
    }

    @Test
    public void testGenerateId_shouldUseCorrectApplicationName() {
        when(externalIdGenerationService.isEnabled()).thenReturn(true);
        when(externalIdGenerationService.executeExternalDefaultGenerateIdMethod(
            eq("jans-auth"), eq(IdType.CLIENTS), eq("")))
            .thenReturn("test-id");
        
        inumService.generateId(IdType.CLIENTS);
        
        verify(externalIdGenerationService).executeExternalDefaultGenerateIdMethod(
            "jans-auth", IdType.CLIENTS, "");
    }

    @Test
    public void testGenerateId_withDifferentIdTypes_shouldWork() {
        when(externalIdGenerationService.isEnabled()).thenReturn(false);
        
        String clientId = inumService.generateId(IdType.CLIENTS);
        String peopleId = inumService.generateId(IdType.PEOPLE);
        
        assertNotNull(clientId);
        assertNotNull(peopleId);
        assertNotEquals(clientId, peopleId);
    }

    @Test
    public void testGenerateId_multipleCalls_shouldGenerateUniqueIds() {
        when(externalIdGenerationService.isEnabled()).thenReturn(false);
        
        String id1 = inumService.generateId(IdType.CLIENTS);
        String id2 = inumService.generateId(IdType.CLIENTS);
        String id3 = inumService.generateId(IdType.CLIENTS);
        
        assertNotEquals(id1, id2);
        assertNotEquals(id2, id3);
        assertNotEquals(id1, id3);
    }

    @Test
    public void testConstants_maxIdgenTryCount() {
        assertEquals(InumService.MAX_IDGEN_TRY_COUNT, 10);
    }
}