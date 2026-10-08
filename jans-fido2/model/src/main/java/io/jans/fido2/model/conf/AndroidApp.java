/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;

/**
 * An Android application allowed to use a relying party's passkeys.
 * <p>
 * Recorded as the single source of truth for the Digital Asset Links ({@code assetlinks.json}) entry that
 * ties the app to the RP ID. The FIDO2 server does not consult it when deciding whether to accept an
 * origin - see {@code CommonVerifiers#verifyRpDomain}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AndroidApp {

    @DocProperty(description = "Android application package name, e.g. com.example.app. "
            + "Not enforced by the server; used to generate assetlinks.json.")
    private String packageName;

    @DocProperty(description = "SHA-256 fingerprints of the app signing certificate, as colon-separated hex "
            + "(AB:CD:...). Not enforced by the server; used to generate assetlinks.json.")
    private List<String> sha256CertFingerprints = new ArrayList<String>();

    @DocProperty(description = "How the app is distributed - play-store or self-signed. "
            + "Not enforced by the server; tells which signing certificate the fingerprints belong to.")
    private String distribution;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getSha256CertFingerprints() {
        return sha256CertFingerprints;
    }

    public void setSha256CertFingerprints(List<String> sha256CertFingerprints) {
        this.sha256CertFingerprints = sha256CertFingerprints;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    @Override
    public String toString() {
        return "AndroidApp [packageName=" + packageName + ", sha256CertFingerprints=" + sha256CertFingerprints
                + ", distribution=" + distribution + "]";
    }
}
