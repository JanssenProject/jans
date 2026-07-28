/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.trust.AttestationTrustConfig;
import io.jans.fido2.service.mds.AttestationCertificateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Assembles the read-only attestation status reported by the trust endpoints.
 * <p>
 * This service only reads state that the attestation service already holds; it never changes
 * attestation behaviour.
 *
 * @author Janssen Project
 */
@ApplicationScoped
public class TrustStatusService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    /**
     * The attestation policy the server is actually applying.
     */
    public AttestationTrustConfig getAttestationTrustConfig() {
        AttestationTrustConfig trustConfig = new AttestationTrustConfig();
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            log.warn("Fido2 configuration is not available; reporting empty attestation trust config");
            return trustConfig;
        }

        String configuredMode = fido2Configuration.getAttestationMode();
        trustConfig.setAttestationMode(configuredMode);
        trustConfig.setAttestationModeRecognized(AttestationMode.getByValue(configuredMode) != null);
        // Only "enforced" applies the stricter MDS trust rules (AttestationCertificateService
        // .isAttestationEnforced) — "disabled" and "monitor" both stay lenient, so anything other than
        // enforced still accepts an authenticator that fails attestation validation.
        trustConfig.setUnattestedAuthenticatorsAllowed(!isAttestationEnforced(configuredMode));
        trustConfig.setEnterpriseAttestation(fido2Configuration.isEnterpriseAttestation());
        trustConfig.setMetadataServiceDisabled(fido2Configuration.isDisableMetadataService());
        trustConfig.setAppleRootCaPresent(attestationCertificateService.isAppleRootCaPresent());
        trustConfig.setEnabledFidoAlgorithms(fido2Configuration.getEnabledFidoAlgorithms());
        trustConfig.setHints(fido2Configuration.getHints());

        return trustConfig;
    }

    private boolean isAttestationEnforced(String configuredMode) {
        return AttestationMode.ENFORCED.getValue().equalsIgnoreCase(configuredMode);
    }
}
