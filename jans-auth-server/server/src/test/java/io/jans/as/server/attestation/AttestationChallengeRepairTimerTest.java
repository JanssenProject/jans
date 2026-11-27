/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.attestation;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cdi.event.AttestationChallengeRepairEvent;

@Listeners(MockitoTestNGListener.class)
public class AttestationChallengeRepairTimerTest {

    @InjectMocks
    private AttestationChallengeRepairTimer timer;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private AttestationChallengeService challengeService;

    @Mock
    private AttestationStore store;

    @BeforeMethod
    public void setUp() {
        lenient().when(appConfiguration.getAttestationChallengeRepairIntervalSeconds()).thenReturn(1);
    }

    @Test
    public void process_whenFeatureEnabledOnSql_repairsLeases() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ATTESTATION)).thenReturn(true);
        when(store.isTransactional()).thenReturn(true);

        timer.process(new AttestationChallengeRepairEvent());

        verify(challengeService).repairExpiredLeases();
    }

    @Test
    public void process_whenFeatureDisabled_doesNothing() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ATTESTATION)).thenReturn(false);

        timer.process(new AttestationChallengeRepairEvent());

        verify(challengeService, never()).repairExpiredLeases();
    }

    @Test
    public void isDue_onlyOnTransactionalBackend() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ATTESTATION)).thenReturn(true);
        when(store.isTransactional()).thenReturn(false);
        assertFalse(timer.isDue());

        when(store.isTransactional()).thenReturn(true);
        assertTrue(timer.isDue());
    }

    @Test
    public void process_repairFailureIsContained() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ATTESTATION)).thenReturn(true);
        when(store.isTransactional()).thenReturn(true);
        when(challengeService.repairExpiredLeases()).thenThrow(new IllegalStateException("db down"));

        timer.process(new AttestationChallengeRepairEvent());
    }
}
