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
 * This service only reads state the attestation and metadata services already hold. It never changes
 * attestation behaviour, never triggers a metadata download, and never touches the document store —
 * a diagnostics endpoint must not cause outbound traffic or mutate state.
 *
 * @author Janssen Project
 */
@ApplicationScoped
public class TrustStatusService {

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    @Inject
    private TocService tocService;

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
     * The state of the metadata the server validates attestation against.
     * <p>
     * Every value is read from memory. In particular the blob's {@code nextUpdate} comes from
     * {@link TocService#getLoadedTocNextUpdate()} rather than {@code getNextUpdateDate()}, which reads
     * the document store and throws on failure — unusable from a health endpoint.
     */
    public MdsHealth getMdsHealth() {
        MdsHealth health = new MdsHealth();
        health.setTimestamp(LocalDateTime.now(ZoneOffset.UTC).format(ISO_DATE_TIME));

        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            log.error("Fido2 configuration is not available; reporting MDS as DOWN");
            health.setStatus(MdsHealthStatus.DOWN);
            health.setBlobExpired(true);
            health.setLastRefreshError("Fido2 configuration is not available");
            return health;
        }

        boolean metadataServiceDisabled = fido2Configuration.isDisableMetadataService();
        health.setMetadataServiceDisabled(metadataServiceDisabled);
        health.setMetadataServers(toMetadataServerStatus(fido2Configuration.getMetadataServers()));
        health.setTocEntryCount(tocService.getTocEntryCount());
        health.setLastRefreshError(tocService.getLastRefreshError());

        LocalDate nextUpdate = tocService.getLoadedTocNextUpdate();
        if (nextUpdate != null) {
            health.setNextUpdate(nextUpdate.toString());
        }
        // Same rule the server uses to decide a re-download is due: no blob loaded, or its nextUpdate
        // is today or earlier (see TocService.fetchMetadataOnce, which skips the download only while
        // nextUpdate is strictly after today).
        health.setBlobExpired(nextUpdate == null || !nextUpdate.isAfter(LocalDate.now()));

        LocalDateTime lastSuccessfulRefresh = tocService.getLastSuccessfulRefresh();
        if (lastSuccessfulRefresh != null) {
            health.setLastSuccessfulRefresh(lastSuccessfulRefresh.format(ISO_DATE_TIME));
        }

        health.setStatus(resolveStatus(health));
        return health;
    }

    /**
     * DOWN means the metadata is unusable right now — nothing is loaded, or what is loaded is past the
     * validity window the FIDO Alliance declared for it.
     * <p>
     * A failed refresh on its own is deliberately <em>not</em> DOWN. While the cached blob is still
     * inside its validity window attestation validation works normally, so returning 503 would page an
     * operator for a service that is functioning. That condition is still surfaced — as
     * {@code lastRefreshError} on a 200 response — and is the signal to alert on for early warning
     * that the metadata is heading towards expiry.
     * <p>
     * A metadata service switched off by configuration is a deliberate choice, not an outage, and is
     * reported as DISABLED so a monitor wired to this endpoint does not page for it.
     */
    private MdsHealthStatus resolveStatus(MdsHealth health) {
        if (health.isMetadataServiceDisabled()) {
            return MdsHealthStatus.DISABLED;
        }
        if (health.getTocEntryCount() == 0 || health.isBlobExpired()) {
            return MdsHealthStatus.DOWN;
        }
        return MdsHealthStatus.UP;
    }

    private List<MetadataServerStatus> toMetadataServerStatus(List<MetadataServer> metadataServers) {
        List<MetadataServerStatus> statuses = new ArrayList<>();
        if (metadataServers == null) {
            return statuses;
        }
        for (MetadataServer metadataServer : metadataServers) {
            // Presence only — the configured trust anchor must never leave the server.
            statuses.add(new MetadataServerStatus(metadataServer.getUrl(),
                    StringHelper.isNotEmpty(metadataServer.getRootCert())));
        }
        return statuses;
    }

    private boolean isAttestationEnforced(String configuredMode) {
        return AttestationMode.ENFORCED.getValue().equalsIgnoreCase(configuredMode);
    }
}
