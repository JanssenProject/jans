/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseEdDSAAlgorithm;
import io.jans.fido2.ctap.CoseMLDSAAlgorithm;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.model.common.PublicKeyCredentialParameters;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.verifier.SignatureVerifier;

/**
 * The advertised algorithm set must be derived from what this server can actually complete a
 * registration with, not from a literal list. These tests pin that: an algorithm reaches
 * pubKeyCredParams only when the decoder can build its key and the verifier can check its signature.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttestationServiceAlgorithmSelectionTest {

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private CoseService coseService;

    @Mock
    private SignatureVerifier signatureVerifier;

    @InjectMocks
    private AttestationService attestationService;

    private final Fido2Configuration fido2Configuration = new Fido2Configuration();

    @BeforeEach
    void stubConfiguration() {
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        supportEverything();
    }

    private void supportEverything() {
        when(coseService.isDecodable(anyInt())).thenReturn(true);
        when(signatureVerifier.isSupported(anyInt())).thenReturn(true);
    }

    private Set<Integer> advertisedAlgorithms() {
        return attestationService.preparePublicKeyCredentialSelection().stream()
                .map(PublicKeyCredentialParameters::getAlg).collect(Collectors.toSet());
    }

    @Test
    void whenNothingConfigured_advertisesTheSupportedDefaults() {
        fido2Configuration.setEnabledFidoAlgorithms(null);

        assertEquals(Set.of(CoseRSAAlgorithm.RS256.getNumericValue(), CoseEC2Algorithm.ES256.getNumericValue(),
                CoseEdDSAAlgorithm.EdDSA.getNumericValue()), advertisedAlgorithms());
    }

    @Test
    void whenNothingConfigured_omitsADefaultTheProviderCannotVerify() {
        // The FIPS build's provider supports strictly less than the standard one, so a default the
        // deployment cannot honour must be dropped rather than advertised and failed later.
        fido2Configuration.setEnabledFidoAlgorithms(null);
        when(signatureVerifier.isSupported(CoseEdDSAAlgorithm.EdDSA.getNumericValue())).thenReturn(false);

        Set<Integer> advertised = advertisedAlgorithms();

        assertEquals(Set.of(CoseRSAAlgorithm.RS256.getNumericValue(), CoseEC2Algorithm.ES256.getNumericValue()),
                advertised);
    }

    @Test
    void whenConfigured_advertisesOnlyTheConfiguredAlgorithms() {
        fido2Configuration.setEnabledFidoAlgorithms(List.of("RS512", "ES384"));

        assertEquals(Set.of(CoseRSAAlgorithm.RS512.getNumericValue(), CoseEC2Algorithm.ES384.getNumericValue()),
                advertisedAlgorithms());
    }

    @Test
    void whenConfiguredAlgorithmIsNotDecodable_itIsSkippedForTheNextInItsFamily() {
        fido2Configuration.setEnabledFidoAlgorithms(Arrays.asList("ECDH_ES_HKDF_256", "ES256"));
        when(coseService.isDecodable(CoseEC2Algorithm.ECDH_ES_HKDF_256.getNumericValue())).thenReturn(false);

        assertEquals(Set.of(CoseEC2Algorithm.ES256.getNumericValue()), advertisedAlgorithms());
    }

    @Test
    void whenNoConfiguredAlgorithmIsSupported_fallsBackToTheDefaultsAndLogs() {
        fido2Configuration.setEnabledFidoAlgorithms(List.of("ES256"));
        when(signatureVerifier.isSupported(anyInt())).thenReturn(false);

        // Nothing is supported at all here, so the fallback finds no default either - the point is that
        // an empty configured result does not silently become an empty pubKeyCredParams.
        assertTrue(advertisedAlgorithms().isEmpty());
        verify(log).error(contains("None of the configured enabledFidoAlgorithms"), any(Object.class));
    }

    /**
     * ML-DSA ships on the standard build only, so the standard-only decision rests on this: an ML-DSA name
     * reaches pubKeyCredParams when the running provider can honour it, and is dropped when it cannot.
     */
    @ParameterizedTest(name = "{0} is advertised when the provider supports it")
    @CsvSource({ "ML-DSA-44, -48", "ML-DSA-65, -49", "ML-DSA-87, -50" })
    void whenProviderSupportsMlDsa_itIsAdvertised(String configuredName, int codePoint) {
        fido2Configuration.setEnabledFidoAlgorithms(List.of(configuredName));

        assertEquals(Set.of(codePoint), advertisedAlgorithms());
    }

    /** The underscore spelling has to resolve too, since it is what a Java-shaped config would carry. */
    @Test
    void whenMlDsaIsConfiguredWithUnderscores_itIsAdvertised() {
        fido2Configuration.setEnabledFidoAlgorithms(List.of("ML_DSA_65"));

        assertEquals(Set.of(CoseMLDSAAlgorithm.ML_DSA_65.getNumericValue()), advertisedAlgorithms());
    }

    /**
     * The FIPS build's provider has no ML-DSA, so isSupported reports false and the algorithm must not be
     * offered. Advertising it there would fail the ceremony after the authenticator picked it - the
     * advertise-then-fail shape this tree exists to remove.
     */
    @ParameterizedTest(name = "{0} is not advertised when the provider lacks it")
    @CsvSource({ "ML-DSA-44, -48", "ML-DSA-65, -49", "ML-DSA-87, -50" })
    void whenProviderLacksMlDsa_itIsNotAdvertised(String configuredName, int codePoint) {
        fido2Configuration.setEnabledFidoAlgorithms(List.of(configuredName));
        when(signatureVerifier.isSupported(codePoint)).thenReturn(false);

        Set<Integer> advertised = advertisedAlgorithms();

        assertTrue(!advertised.contains(codePoint), configuredName + " must not be advertised without provider support");
        verify(log).error(contains("not supported by this server"), any(Object.class));
    }

    /**
     * A FIPS deployment can share a configuration with a standard one: the algorithms its provider does
     * support still come through, and only ML-DSA drops out.
     */
    @Test
    void whenProviderLacksMlDsa_theRestOfTheConfigurationStillApplies() {
        fido2Configuration.setEnabledFidoAlgorithms(List.of("ES256", "ML-DSA-65"));
        when(signatureVerifier.isSupported(CoseMLDSAAlgorithm.ML_DSA_65.getNumericValue())).thenReturn(false);

        assertEquals(Set.of(CoseEC2Algorithm.ES256.getNumericValue()), advertisedAlgorithms());
    }

    @Test
    void whenConfiguredNameIsUnknown_itIsIgnored() {
        fido2Configuration.setEnabledFidoAlgorithms(Arrays.asList("NOT_AN_ALGORITHM", "RS256"));

        assertEquals(Set.of(CoseRSAAlgorithm.RS256.getNumericValue()), advertisedAlgorithms());
    }
}
