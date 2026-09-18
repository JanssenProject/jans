/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.conf.RequestedPartyPolicy;

/**
 * The resolution rule carries the backward-compatibility guarantee for per-RP policy: a relying party with
 * no policy of its own must behave exactly as it did when every decision was global. Stating that once,
 * here, is why the eight enforcement points can each call a single method and not restate the fallback.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RpPolicyServiceTest {

    private static final String RP_ORIGIN = "high-assurance.example.com";

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @InjectMocks
    private RpPolicyService rpPolicyService;

    private final Fido2Configuration fido2Configuration = new Fido2Configuration();

    @BeforeEach
    void stubConfiguration() {
        fido2Configuration.setAttestationMode(AttestationMode.MONITOR.getValue());
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
    }

    private RequestedParty rp(String origin, String attestationMode) {
        RequestedParty requestedParty = new RequestedParty();
        requestedParty.setId(origin);
        requestedParty.setOrigins(new ArrayList<>(Collections.singletonList(origin)));
        if (attestationMode != null) {
            RequestedPartyPolicy policy = new RequestedPartyPolicy();
            policy.setAttestationMode(attestationMode);
            requestedParty.setPolicy(policy);
        }

        return requestedParty;
    }

    private void configureParties(RequestedParty... parties) {
        fido2Configuration.setRequestedParties(new ArrayList<>(Arrays.asList(parties)));
    }

    @Test
    void whenTheRpSetsAMode_itOverridesTheGlobalOne() {
        configureParties(rp(RP_ORIGIN, AttestationMode.ENFORCED.getValue()));

        assertEquals(AttestationMode.ENFORCED.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));
    }

    /**
     * The guarantee #14516 calls mandatory: an RP that predates per-RP policy is untouched by it.
     */
    @Test
    void whenTheRpHasNoPolicy_theGlobalModeApplies() {
        configureParties(rp(RP_ORIGIN, null));

        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));
    }

    @Test
    void whenTheRpHasAPolicyButNoMode_theGlobalModeApplies() {
        RequestedParty requestedParty = rp(RP_ORIGIN, null);
        requestedParty.setPolicy(new RequestedPartyPolicy());
        configureParties(requestedParty);

        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));
    }

    /** A blank value is not a choice, so it must not shadow the global mode. */
    @ParameterizedTest(name = "blank mode [{0}] falls back")
    @ValueSource(strings = { "", "   " })
    void whenTheRpModeIsBlank_theGlobalModeApplies(String blank) {
        configureParties(rp(RP_ORIGIN, blank));

        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));
    }

    @Test
    void whenTheOriginMatchesNoRp_theGlobalModeApplies() {
        configureParties(rp(RP_ORIGIN, AttestationMode.ENFORCED.getValue()));

        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode("other.example.com"));
    }

    /**
     * One RP's stricter policy must not leak onto its neighbours - that separation is the whole point.
     */
    @Test
    void oneRpPolicyDoesNotAffectAnother() {
        configureParties(rp(RP_ORIGIN, AttestationMode.ENFORCED.getValue()),
                rp("consumer.example.com", null));

        assertEquals(AttestationMode.ENFORCED.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));
        assertEquals(AttestationMode.MONITOR.getValue(),
                rpPolicyService.resolveAttestationMode("consumer.example.com"));
    }

    @Test
    void originMatchingIgnoresCase() {
        configureParties(rp(RP_ORIGIN, AttestationMode.ENFORCED.getValue()));

        assertEquals(AttestationMode.ENFORCED.getValue(),
                rpPolicyService.resolveAttestationMode("High-Assurance.Example.COM"));
    }

    /**
     * Every shape a deployment can legitimately be in before any RP is configured, and a malformed entry,
     * must resolve to the global mode rather than throw on the ceremony path.
     */
    @Test
    void missingOrMalformedConfigurationFallsBackWithoutThrowing() {
        fido2Configuration.setRequestedParties(null);
        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));

        fido2Configuration.setRequestedParties(new ArrayList<>());
        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));

        RequestedParty withoutOrigins = new RequestedParty();
        withoutOrigins.setOrigins(null);
        configureParties(withoutOrigins);
        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));

        List<RequestedParty> withNull = new ArrayList<>();
        withNull.add(null);
        fido2Configuration.setRequestedParties(withNull);
        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(RP_ORIGIN));
    }

    @Test
    void aMissingOriginResolvesToTheGlobalMode() {
        configureParties(rp(RP_ORIGIN, AttestationMode.ENFORCED.getValue()));

        assertEquals(AttestationMode.MONITOR.getValue(), rpPolicyService.resolveAttestationMode(null));
    }

    @Test
    void whenThereIsNoFido2Configuration_nothingResolvesAndNothingThrows() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);

        assertNull(rpPolicyService.resolveAttestationMode(RP_ORIGIN));
    }
}
