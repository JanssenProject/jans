/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static io.jans.fido2.service.readiness.ExpectedAssociationFileBuilder.LOGIN_CREDS_RELATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.fido2.model.conf.AndroidApp;
import io.jans.fido2.model.conf.IosApp;
import io.jans.fido2.model.conf.RequestedParty;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Compares a deployed {@code assetlinks.json} / {@code apple-app-site-association} with what the RP
 * configuration says it should contain. Works on the body of a fetch already made, so it never fetches.
 * <p>
 * Every reason names only a configured value (package, fingerprint, {@code TeamID.BundleID}) or a count,
 * never deployed content and never parser text. Check ids and rules:
 * <ul>
 * <li>{@code content.parse}: FAIL when the body is empty, not valid JSON (this includes a CMS-signed
 * AASA), too deeply nested, or not the expected top-level shape (array for assetlinks, object for AASA).
 * Nothing else is reported for that file.</li>
 * <li>{@code content.android.package}: per configured app, PASS if a statement lists the package, else
 * FAIL. No Android apps configured: one PASS saying so, and the Android checks below are skipped.</li>
 * <li>{@code content.android.fingerprint}: per configured app, FAIL for each configured fingerprint the
 * file lacks (compared as case-insensitive hex, colons optional); WARN with a count if the file lists
 * fingerprints that are not configured; PASS otherwise.</li>
 * <li>{@code content.android.relation}: per configured app, FAIL unless a statement for the package carries
 * {@code delegate_permission/common.get_login_creds}.</li>
 * <li>{@code content.ios.app}: per configured iOS app, FAIL unless {@code webcredentials.apps} contains
 * {@code TeamID.BundleID} (case-insensitive). No iOS apps configured: one PASS saying so.</li>
 * <li>{@code content.drift}: WARN with counts when the deployed file differs from the file Jans would
 * generate ({@link ExpectedAssociationFileBuilder}), for example after a signing key rotation. Order,
 * whitespace and unrelated extra content are ignored. Never a FAIL. Skipped when no apps are configured
 * for the platform.</li>
 * <li>{@code content.ios.cdn}: an informational WARN, always emitted for the Apple file whether the other
 * checks pass or fail: iOS reads the file through {@code app-site-association.cdn-apple.com}, which caches
 * aggressively, so a correct file may not take effect immediately. This class does not contact that CDN.</li>
 * </ul>
 */
@ApplicationScoped
public class ContentChecker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<CheckResult> checkAssetLinks(RequestedParty party, byte[] body) {
        List<CheckResult> checks = new ArrayList<>();
        JsonNode deployed = parse(body, true, checks);
        if (deployed == null) {
            return checks;
        }
        if (party.getAndroidApps().isEmpty()) {
            checks.add(pass("content.android.package", "no Android apps configured"));
            return checks;
        }
        Set<String> found = androidFacts(deployed);
        for (AndroidApp app : party.getAndroidApps()) {
            String pkg = app.getPackageName();
            checks.add(found.contains("P|" + pkg) ? pass("content.android.package", "package " + pkg + " listed")
                    : fail("content.android.package", "package " + pkg + " not listed"));
            checks.addAll(fingerprintChecks(app, found));
            checks.add(found.contains("R|" + pkg)
                    ? pass("content.android.relation", "package " + pkg + " has the login credentials relation")
                    : fail("content.android.relation", "package " + pkg + " lacks " + LOGIN_CREDS_RELATION));
        }
        checks.add(drift(androidFacts(ExpectedAssociationFileBuilder.assetLinks(party)), found));
        return checks;
    }

    public List<CheckResult> checkAppleAppSiteAssociation(RequestedParty party, byte[] body) {
        List<CheckResult> checks = new ArrayList<>();
        JsonNode deployed = parse(body, false, checks);
        if (deployed != null) {
            if (party.getIosApps().isEmpty()) {
                checks.add(pass("content.ios.app", "no iOS apps configured"));
            } else {
                Set<String> found = iosFacts(deployed);
                for (IosApp app : party.getIosApps()) {
                    String id = ExpectedAssociationFileBuilder.iosAppId(app);
                    checks.add(found.contains(id.toUpperCase(Locale.ROOT))
                            ? pass("content.ios.app", "app " + id + " listed in webcredentials.apps")
                            : fail("content.ios.app", "app " + id + " not listed in webcredentials.apps"));
                }
                checks.add(drift(iosFacts(ExpectedAssociationFileBuilder.appleAppSiteAssociation(party)), found));
            }
        }
        checks.add(new CheckResult("content.ios.cdn", CheckStatus.WARN,
                "iOS reads this file through app-site-association.cdn-apple.com, which caches aggressively; "
                        + "a correct file may not take effect immediately"));
        return checks;
    }

    private static List<CheckResult> fingerprintChecks(AndroidApp app, Set<String> found) {
        String pkg = app.getPackageName();
        List<CheckResult> checks = new ArrayList<>();
        Set<String> configured = new HashSet<>();
        for (String fingerprint : app.getSha256CertFingerprints()) {
            String normalized = ExpectedAssociationFileBuilder.normalizeFingerprint(fingerprint);
            configured.add(normalized);
            if (!found.contains("F|" + pkg + "|" + normalized)) {
                checks.add(fail("content.android.fingerprint",
                        "fingerprint " + fingerprint + " for package " + pkg + " not listed"));
            }
        }
        String prefix = "F|" + pkg + "|";
        long unconfigured = found.stream().filter(f -> f.startsWith(prefix))
                .filter(f -> !configured.contains(f.substring(prefix.length()))).count();
        if (unconfigured > 0) {
            checks.add(new CheckResult("content.android.fingerprint", CheckStatus.WARN,
                    "file lists " + unconfigured + " fingerprint(s) for package " + pkg + " not in configuration"));
        }
        if (checks.isEmpty()) {
            checks.add(pass("content.android.fingerprint", "all configured fingerprints for package " + pkg + " listed"));
        }
        return checks;
    }

    private static CheckResult drift(Set<String> expected, Set<String> deployed) {
        long missing = expected.stream().filter(f -> !deployed.contains(f)).count();
        long extra = deployed.stream().filter(f -> !expected.contains(f)).count();
        if (missing == 0 && extra == 0) {
            return pass("content.drift", "deployed file matches what Jans would generate");
        }
        return new CheckResult("content.drift", CheckStatus.WARN, "deployed file differs from what Jans would "
                + "generate: " + missing + " expected entr" + (missing == 1 ? "y" : "ies") + " missing, " + extra
                + " unexpected");
    }

    /**
     * Flattens an assetlinks document into comparable facts: {@code P|pkg} (package listed), {@code F|pkg|FP}
     * (normalized fingerprint) and {@code R|pkg} (login credentials relation present).
     */
    private static Set<String> androidFacts(JsonNode statements) {
        Set<String> facts = new HashSet<>();
        for (JsonNode statement : statements) {
            JsonNode target = statement.path("target");
            if (!"android_app".equals(target.path("namespace").asText()) || !target.path("package_name").isTextual()) {
                continue;
            }
            String pkg = target.path("package_name").asText();
            facts.add("P|" + pkg);
            for (JsonNode fingerprint : target.path("sha256_cert_fingerprints")) {
                if (fingerprint.isTextual()) {
                    facts.add("F|" + pkg + "|" + ExpectedAssociationFileBuilder.normalizeFingerprint(fingerprint.asText()));
                }
            }
            for (JsonNode relation : statement.path("relation")) {
                if (LOGIN_CREDS_RELATION.equals(relation.asText())) {
                    facts.add("R|" + pkg);
                }
            }
        }
        return facts;
    }

    /** The upper-cased {@code webcredentials.apps} entries. */
    private static Set<String> iosFacts(JsonNode association) {
        Set<String> facts = new HashSet<>();
        for (JsonNode app : association.path("webcredentials").path("apps")) {
            if (app.isTextual()) {
                facts.add(app.asText().toUpperCase(Locale.ROOT));
            }
        }
        return facts;
    }

    /** Parses the body, or adds one fixed-reason FAIL and returns null. */
    private static JsonNode parse(byte[] body, boolean expectArray, List<CheckResult> checks) {
        JsonNode node;
        try {
            node = MAPPER.readTree(body);
        } catch (IOException | RuntimeException e) {
            checks.add(fail("content.parse", "not valid JSON"));
            return null;
        }
        if (node == null || node.isMissingNode()) {
            checks.add(fail("content.parse", "not valid JSON"));
            return null;
        }
        if (expectArray ? !node.isArray() : !node.isObject()) {
            checks.add(fail("content.parse", expectArray ? "not a JSON array of statements" : "not a JSON object"));
            return null;
        }
        return node;
    }

    private static CheckResult pass(String id, String reason) {
        return new CheckResult(id, CheckStatus.PASS, reason);
    }

    private static CheckResult fail(String id, String reason) {
        return new CheckResult(id, CheckStatus.FAIL, reason);
    }
}
