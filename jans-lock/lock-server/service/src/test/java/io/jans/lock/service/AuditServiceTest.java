/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.service;

import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.OrganizationService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for AuditService
 * 
 * @author Janssen Project
 */
public class AuditServiceTest {

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @Mock
    private BaseDnConfiguration baseDnConfiguration;

    @InjectMocks
    private AuditService auditService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);
        when(baseDnConfiguration.getAudit()).thenReturn("ou=audit,o=jans");
        when(organizationService.getBaseDn()).thenReturn("o=jans");
    }

    @Test
    public void testAddLogEntry_withValidEntry_shouldPersist() {
        LogEntry logEntry = new LogEntry();
        when(persistenceEntryManager.contains(anyString(), eq(LogEntry.class))).thenReturn(false);
        
        auditService.addLogEntry(logEntry);
        
        assertNotNull(logEntry.getInum());
        assertNotNull(logEntry.getDn());
        assertTrue(logEntry.getDn().contains("ou=log"));
        verify(persistenceEntryManager).persist(logEntry);
    }

    @Test
    public void testAddLogEntry_withNull_shouldNotPersist() {
        auditService.addLogEntry(null);
        
        verify(persistenceEntryManager, never()).persist(any());
    }

    @Test
    public void testAddLogEntry_shouldGenerateUniqueInums() {
        LogEntry entry1 = new LogEntry();
        LogEntry entry2 = new LogEntry();
        when(persistenceEntryManager.contains(anyString(), eq(LogEntry.class))).thenReturn(false);
        
        auditService.addLogEntry(entry1);
        auditService.addLogEntry(entry2);
        
        assertNotEquals(entry1.getInum(), entry2.getInum());
    }

    @Test
    public void testAddTelemetryEntry_withValidEntry_shouldPersist() {
        TelemetryEntry telemetryEntry = new TelemetryEntry();
        when(persistenceEntryManager.contains(anyString(), eq(TelemetryEntry.class))).thenReturn(false);
        
        auditService.addTelemetryEntry(telemetryEntry);
        
        assertNotNull(telemetryEntry.getInum());
        assertNotNull(telemetryEntry.getDn());
        assertTrue(telemetryEntry.getDn().contains("ou=telemetry"));
        verify(persistenceEntryManager).persist(telemetryEntry);
    }

    @Test
    public void testAddTelemetryEntry_withNull_shouldNotPersist() {
        auditService.addTelemetryEntry(null);
        
        verify(persistenceEntryManager, never()).persist(any());
    }

    @Test
    public void testAddHealthEntry_withValidEntry_shouldPersist() {
        HealthEntry healthEntry = new HealthEntry();
        when(persistenceEntryManager.contains(anyString(), eq(HealthEntry.class))).thenReturn(false);
        
        auditService.addHealthEntry(healthEntry);
        
        assertNotNull(healthEntry.getInum());
        assertNotNull(healthEntry.getDn());
        assertTrue(healthEntry.getDn().contains("ou=health"));
        verify(persistenceEntryManager).persist(healthEntry);
    }

    @Test
    public void testAddHealthEntry_withNull_shouldNotPersist() {
        auditService.addHealthEntry(null);
        
        verify(persistenceEntryManager, never()).persist(any());
    }

    @Test
    public void testGetDnForLogEntry_shouldReturnCorrectFormat() {
        String inum = "test-inum";
        
        String dn = auditService.getDnForLogEntry(inum);
        
        assertEquals(dn, "inum=test-inum,ou=log,ou=audit,o=jans");
    }

    @Test
    public void testGetDnForTelemetryEntry_shouldReturnCorrectFormat() {
        String inum = "test-inum";
        
        String dn = auditService.getDnForTelemetryEntry(inum);
        
        assertEquals(dn, "inum=test-inum,ou=telemetry,ou=audit,o=jans");
    }

    @Test
    public void testGetDnForHealthEntry_shouldReturnCorrectFormat() {
        String inum = "test-inum";
        
        String dn = auditService.getDnForHealthEntry(inum);
        
        assertEquals(dn, "inum=test-inum,ou=health,ou=audit,o=jans");
    }

    @Test
    public void testGenerateInumForEntry_withNoCollision_shouldReturnFirstGeneratedInum() {
        when(persistenceEntryManager.contains(anyString(), eq(LogEntry.class))).thenReturn(false);
        
        String inum = auditService.generateInumForEntry("inum=%s,ou=log,%s", LogEntry.class);
        
        assertNotNull(inum);
        assertTrue(inum.length() >= 36); // UUID format
    }

    @Test
    public void testGenerateInumForEntry_withCollision_shouldRetryAndReturnUniqueInum() {
        when(persistenceEntryManager.contains(anyString(), eq(LogEntry.class)))
            .thenReturn(true)   // First attempt collides
            .thenReturn(true)   // Second attempt collides
            .thenReturn(false); // Third attempt succeeds
        
        String inum = auditService.generateInumForEntry("inum=%s,ou=log,%s", LogEntry.class);
        
        assertNotNull(inum);
        verify(persistenceEntryManager, times(3)).contains(anyString(), eq(LogEntry.class));
    }

    @Test
    public void testGenerateInumForEntry_multipleCalls_shouldGenerateUniqueValues() {
        when(persistenceEntryManager.contains(anyString(), any())).thenReturn(false);
        
        String inum1 = auditService.generateInumForEntry("inum=%s,ou=log,%s", LogEntry.class);
        String inum2 = auditService.generateInumForEntry("inum=%s,ou=log,%s", LogEntry.class);
        String inum3 = auditService.generateInumForEntry("inum=%s,ou=log,%s", LogEntry.class);
        
        assertNotEquals(inum1, inum2);
        assertNotEquals(inum2, inum3);
        assertNotEquals(inum1, inum3);
    }

    @Test
    public void testAddLogEntry_shouldSetCorrectDnFormat() {
        LogEntry logEntry = new LogEntry();
        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        when(persistenceEntryManager.contains(anyString(), eq(LogEntry.class))).thenReturn(false);
        
        auditService.addLogEntry(logEntry);
        
        verify(persistenceEntryManager).persist(captor.capture());
        LogEntry persistedEntry = captor.getValue();
        assertTrue(persistedEntry.getDn().matches("inum=.+,ou=log,ou=audit,o=jans"));
    }

    @Test
    public void testAddTelemetryEntry_shouldSetCorrectDnFormat() {
        TelemetryEntry telemetryEntry = new TelemetryEntry();
        ArgumentCaptor<TelemetryEntry> captor = ArgumentCaptor.forClass(TelemetryEntry.class);
        when(persistenceEntryManager.contains(anyString(), eq(TelemetryEntry.class))).thenReturn(false);
        
        auditService.addTelemetryEntry(telemetryEntry);
        
        verify(persistenceEntryManager).persist(captor.capture());
        TelemetryEntry persistedEntry = captor.getValue();
        assertTrue(persistedEntry.getDn().matches("inum=.+,ou=telemetry,ou=audit,o=jans"));
    }

    @Test
    public void testAddHealthEntry_shouldSetCorrectDnFormat() {
        HealthEntry healthEntry = new HealthEntry();
        ArgumentCaptor<HealthEntry> captor = ArgumentCaptor.forClass(HealthEntry.class);
        when(persistenceEntryManager.contains(anyString(), eq(HealthEntry.class))).thenReturn(false);
        
        auditService.addHealthEntry(healthEntry);
        
        verify(persistenceEntryManager).persist(captor.capture());
        HealthEntry persistedEntry = captor.getValue();
        assertTrue(persistedEntry.getDn().matches("inum=.+,ou=health,ou=audit,o=jans"));
    }

    @Test
    public void testGetDnForLogEntry_withSpecialCharacters_shouldHandleCorrectly() {
        String inumWithSpecial = "test-inum-123-abc";
        
        String dn = auditService.getDnForLogEntry(inumWithSpecial);
        
        assertTrue(dn.contains(inumWithSpecial));
        assertTrue(dn.startsWith("inum="));
    }

    @Test
    public void testGenerateInumForEntry_shouldUseCorrectBaseDn() {
        when(persistenceEntryManager.contains(anyString(), eq(LogEntry.class))).thenReturn(false);
        when(organizationService.getBaseDn()).thenReturn("o=custom");
        
        auditService.generateInumForEntry("inum=%s,ou=log,%s", LogEntry.class);
        
        verify(organizationService).getBaseDn();
    }
}
