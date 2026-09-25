/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.conf.RequestedPartyPolicy;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Resolves an assurance decision for one relying party, falling back to the global configuration.
 * <p>
 * Before per-RP policy existed every decision came from {@link Fido2Configuration}, so the fallback is
 * what keeps an unconfigured deployment behaving exactly as it did: an RP with no matching entry, or a
 * matching entry whose policy leaves the field unset, resolves to the global value.
 * <p>
 * The origin is matched the same way {@code AttestationService.createRpDomain} matches it, so the policy
 * that applies and the RP that a ceremony is attributed to can never disagree.
 */
@ApplicationScoped
public class RpPolicyService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    /**
     * The attestation mode in force for a ceremony on this origin.
     *
     * @param origin the resolved ceremony origin, as stored on the registration entry; may be null
     * @return the per-RP mode when set, otherwise the global mode
     */
    public String resolveAttestationMode(String origin) {
        RequestedPartyPolicy policy = findPolicy(origin);
        if ((policy != null) && StringUtils.isNotBlank(policy.getAttestationMode())) {
            log.debug("Using per-RP attestation mode {} for origin {}", policy.getAttestationMode(), origin);

            return policy.getAttestationMode();
        }

        return globalAttestationMode();
    }

    private String globalAttestationMode() {
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();

        return (fido2Configuration == null) ? null : fido2Configuration.getAttestationMode();
    }

    /**
     * The policy of the relying party that claims this origin, or null when none does.
     */
    private RequestedPartyPolicy findPolicy(String origin) {
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if ((fido2Configuration == null) || StringUtils.isBlank(origin)) {
            return null;
        }

        List<RequestedParty> requestedParties = fido2Configuration.getRequestedParties();
        if (requestedParties == null) {
            return null;
        }

        for (RequestedParty requestedParty : requestedParties) {
            if ((requestedParty == null) || (requestedParty.getOrigins() == null)) {
                continue;
            }
            for (String allowedOrigin : requestedParty.getOrigins()) {
                if (StringHelper.equalsIgnoreCase(origin, allowedOrigin)) {
                    return requestedParty.getPolicy();
                }
            }
        }

        return null;
    }
}
