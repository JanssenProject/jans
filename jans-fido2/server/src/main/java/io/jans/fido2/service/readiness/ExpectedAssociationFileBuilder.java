/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.model.conf.AndroidApp;
import io.jans.fido2.model.conf.IosApp;
import io.jans.fido2.model.conf.RequestedParty;

/**
 * The one definition of what a relying party's association files should contain, built from its
 * {@link RequestedParty}. Pure: no I/O, no configuration access, nothing is served.
 * <p>
 * Shape: one {@code assetlinks.json} statement per {@code AndroidApp}, carrying all of that app's
 * fingerprints, with the {@code delegate_permission/common.get_login_creds} relation; and an
 * {@code apple-app-site-association} with {@code webcredentials.apps} entries of the form
 * {@code TeamID.BundleID}. The readiness checker compares deployed files against this, and serving the files
 * should reuse it so the two cannot disagree.
 */
public final class ExpectedAssociationFileBuilder {

    public static final String LOGIN_CREDS_RELATION = "delegate_permission/common.get_login_creds";

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private ExpectedAssociationFileBuilder() {
    }

    /** The expected {@code assetlinks.json}: a JSON array with one statement per configured Android app. */
    public static ArrayNode assetLinks(RequestedParty party) {
        ArrayNode statements = JSON.arrayNode();
        for (AndroidApp app : party.getAndroidApps()) {
            ObjectNode target = JSON.objectNode();
            target.put("namespace", "android_app");
            target.put("package_name", app.getPackageName());
            ArrayNode fingerprints = target.putArray("sha256_cert_fingerprints");
            for (String fingerprint : app.getSha256CertFingerprints()) {
                fingerprints.add(normalizeFingerprint(fingerprint));
            }
            ObjectNode statement = JSON.objectNode();
            statement.putArray("relation").add(LOGIN_CREDS_RELATION);
            statement.set("target", target);
            statements.add(statement);
        }
        return statements;
    }

    /** The expected {@code apple-app-site-association}: a JSON object with {@code webcredentials.apps}. */
    public static JsonNode appleAppSiteAssociation(RequestedParty party) {
        ObjectNode root = JSON.objectNode();
        ArrayNode apps = root.putObject("webcredentials").putArray("apps");
        for (IosApp app : party.getIosApps()) {
            apps.add(iosAppId(app));
        }
        return root;
    }

    /** {@code TeamID.BundleID}. */
    public static String iosAppId(IosApp app) {
        return app.getTeamId() + "." + app.getBundleId();
    }

    /** Upper-case colon-separated hex, whatever the input style (with or without colons, any case). */
    public static String normalizeFingerprint(String fingerprint) {
        String hex = fingerprint.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                out.append(':');
            }
            out.append(hex, i, Math.min(i + 2, hex.length()));
        }
        return out.toString();
    }
}
