/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.MetadataServer;
import io.jans.fido2.model.trust.AttestationTrustConfig;
import io.jans.fido2.model.trust.MdsHealth;
import io.jans.fido2.model.trust.MdsHealthStatus;
import io.jans.fido2.model.trust.MetadataServerStatus;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.mds.TocService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Assembles the read-only attestation and MDS status reported by the trust endpoints.
 * <p>
 * This service only reads state that the attestation and metadata services already hold; it never
 * changes attestation behaviour, triggers a metadata download, or touches the document store.
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
    private TocService tocService;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    /**
     * The state of the metadata used for attestation validation.
     */
    public MdsHealth getMdsHealth() {
        MdsHealth health = new MdsHealth();
        health.setTimestamp(LocalDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER));

        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            log.warn("Fido2 configuration is not available; reporting MDS as DOWN");
            health.setStatus(MdsHealthStatus.DOWN);
            health.setBlobExpired(true);
            health.setLastRefreshError("Fido2 configuration is not available");
            return health;
        }

        boolean metadataServiceDisabled = fido2Configuration.isDisableMetadataService();
        health.setMetadataServiceDisabled(metadataServiceDisabled);
        health.setMetadataServers(toMetadataServerStatus(fido2Configuration.getMetadataServers()));

        LocalDate nextUpdate = tocService.getLoadedTocNextUpdate();
        if (nextUpdate != null) {
            health.setNextUpdate(nextUpdate.toString());
        }
        // Matches the rule fetchMetadata() uses to decide a re-download is due: absent, today, or past.
        health.setBlobExpired(nextUpdate == null || !nextUpdate.isAfter(LocalDate.now()));

        health.setTocEntryCount(tocService.getTocEntryCount());
        health.setLastRefreshError(tocService.getLastRefreshError());

        LocalDateTime lastSuccessfulRefresh = tocService.getLastSuccessfulRefresh();
        if (lastSuccessfulRefresh != null) {
            health.setLastSuccessfulRefresh(lastSuccessfulRefresh.format(ISO_FORMATTER));
        }

        health.setStatus(resolveStatus(health, metadataServiceDisabled));

        return health;
    }

    /**
     * Disabled is a deliberate configuration choice, not a failure, so it must not be reported as DOWN.
     */
    private MdsHealthStatus resolveStatus(MdsHealth health, boolean metadataServiceDisabled) {
        if (metadataServiceDisabled) {
            return MdsHealthStatus.DISABLED;
        }
        if (health.getLastRefreshError() != null || health.getTocEntryCount() == 0 || health.isBlobExpired()) {
            return MdsHealthStatus.DOWN;
        }
        return MdsHealthStatus.UP;
    }

    private boolean isAttestationEnforced(String configuredMode) {
        return AttestationMode.ENFORCED.getValue().equalsIgnoreCase(configuredMode);
    }

    private List<MetadataServerStatus> toMetadataServerStatus(List<MetadataServer> metadataServers) {
        List<MetadataServerStatus> statuses = new ArrayList<>();
        if (metadataServers == null) {
            return statuses;
        }
        for (MetadataServer metadataServer : metadataServers) {
            // Report only whether a trust anchor is configured — never the certificate itself.
            statuses.add(new MetadataServerStatus(metadataServer.getUrl(),
                    StringHelper.isNotEmpty(metadataServer.getRootCert())));
        }
        return statuses;
    }
}
