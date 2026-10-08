/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;

/**
 * An iOS application allowed to use a relying party's passkeys.
 * <p>
 * Recorded as the single source of truth for the {@code apple-app-site-association} entry that ties the
 * app to the RP ID. The FIDO2 server does not consult it when deciding whether to accept an origin - see
 * {@code CommonVerifiers#verifyRpDomain}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IosApp {

    @DocProperty(description = "Apple Developer Team ID, e.g. T9A667JL6T. "
            + "Not enforced by the server; used to generate apple-app-site-association.")
    private String teamId;

    @DocProperty(description = "Application bundle identifier, e.g. com.example.app. "
            + "Not enforced by the server; used to generate apple-app-site-association.")
    private String bundleId;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    @Override
    public String toString() {
        return "IosApp [teamId=" + teamId + ", bundleId=" + bundleId + "]";
    }
}
