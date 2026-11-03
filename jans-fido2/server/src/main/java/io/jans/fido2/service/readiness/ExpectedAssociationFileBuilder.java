/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.util.List;
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
        for (AndroidApp app : androidApps(party)) {
            ObjectNode target = JSON.objectNode();
            target.put("namespace", "android_app");
            target.put("package_name", app.getPackageName());
            ArrayNode fingerprints = target.putArray("sha256_cert_fingerprints");
            for (String fingerprint : fingerprints(app)) {
                String normalized = normalizeFingerprint(fingerprint);
                fingerprints.add(normalized != null ? normalized : fingerprint);
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
        for (IosApp app : iosApps(party)) {
            if (isComplete(app)) {
                apps.add(iosAppId(app));
            }
        }
        return root;
    }

    /** The configured Android apps; the model setters accept null, which means none. */
    static List<AndroidApp> androidApps(RequestedParty party) {
        return party.getAndroidApps() == null ? List.of() : party.getAndroidApps();
    }

    /** The configured iOS apps; null means none. */
    static List<IosApp> iosApps(RequestedParty party) {
        return party.getIosApps() == null ? List.of() : party.getIosApps();
    }

    /** The configured fingerprints of an Android app; null means none. */
    static List<String> fingerprints(AndroidApp app) {
        return app.getSha256CertFingerprints() == null ? List.of() : app.getSha256CertFingerprints();
    }

    /** True when both the team id and the bundle id are set; an incomplete app is never written to the file. */
    static boolean isComplete(IosApp app) {
        return app.getTeamId() != null && !app.getTeamId().isBlank()
                && app.getBundleId() != null && !app.getBundleId().isBlank();
    }

    /** {@code TeamID.BundleID}. */
    public static String iosAppId(IosApp app) {
        return app.getTeamId() + "." + app.getBundleId();
    }

    /**
     * Upper-case colon-separated hex, whatever the input style (with or without colons, any case), or
     * {@code null} unless the input is whole hex bytes, optionally colon-separated between bytes.
     */
    public static String normalizeFingerprint(String fingerprint) {
        if (fingerprint == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = fingerprint.length();
        while (i < n) {
            if (i + 2 > n || !isAsciiHex(fingerprint.charAt(i)) || !isAsciiHex(fingerprint.charAt(i + 1))) {
                return null;
            }
            if (out.length() > 0) {
                out.append(':');
            }
            out.append(fingerprint, i, i + 2);
            i += 2;
            // one optional colon between bytes, never trailing
            if (i < n && fingerprint.charAt(i) == ':') {
                i++;
                if (i == n) {
                    return null;
                }
            }
        }
        return out.length() == 0 ? null : out.toString().toUpperCase(Locale.ROOT);
    }

    /** ASCII 0-9, A-F, a-f only; {@code Character.digit} would also accept e.g. fullwidth digits. */
    private static boolean isAsciiHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    /** The normalized fingerprint, or the raw text when it is not valid hex (so it can never match). */
    static String normalizeOrRaw(String fingerprint) {
        String normalized = normalizeFingerprint(fingerprint);
        return normalized != null ? normalized : fingerprint;
    }
}
