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

import com.fasterxml.jackson.databind.DeserializationFeature;
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

    private static final String ID_PACKAGE = "content.android.package";
    private static final String ID_FINGERPRINT = "content.android.fingerprint";
    private static final String ID_RELATION = "content.android.relation";
    private static final String ID_PARSE = "content.parse";
    private static final String ID_IOS_APP = "content.ios.app";
    private static final String ID_DRIFT = "content.drift";
    private static final String PACKAGE = "package ";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public List<CheckResult> checkAssetLinks(RequestedParty party, byte[] body) {
        List<CheckResult> checks = new ArrayList<>();
        if (ExpectedAssociationFileBuilder.androidApps(party).isEmpty()) {
            checks.add(pass(ID_PACKAGE, "no Android apps configured"));
            return checks;
        }
        JsonNode deployed = parse(body, true, checks);
        if (deployed == null) {
            return checks;
        }
        Set<String> found = androidFacts(deployed);
        for (AndroidApp app : ExpectedAssociationFileBuilder.androidApps(party)) {
            String pkg = app.getPackageName();
            checks.add(found.contains("P|" + pkg) ? pass(ID_PACKAGE, PACKAGE + pkg + " listed")
                    : fail(ID_PACKAGE, PACKAGE + pkg + " not listed"));
            checks.addAll(fingerprintChecks(app, ExpectedAssociationFileBuilder.androidApps(party), found));
            checks.add(relationBound(app, found)
                    ? pass(ID_RELATION, PACKAGE + pkg + " has the login credentials relation")
                    : fail(ID_RELATION, PACKAGE + pkg + " lacks " + LOGIN_CREDS_RELATION));
        }
        checks.add(drift(androidFacts(ExpectedAssociationFileBuilder.assetLinks(party)), found));
        return checks;
    }

    public List<CheckResult> checkAppleAppSiteAssociation(RequestedParty party, byte[] body) {
        List<CheckResult> checks = new ArrayList<>();
        if (ExpectedAssociationFileBuilder.iosApps(party).isEmpty()) {
            checks.add(pass(ID_IOS_APP, "no iOS apps configured"));
        } else {
            JsonNode deployed = parse(body, false, checks);
            if (deployed != null) {
                Set<String> found = iosFacts(deployed);
                for (IosApp app : ExpectedAssociationFileBuilder.iosApps(party)) {
                    String id = ExpectedAssociationFileBuilder.iosAppId(app);
                    checks.add(found.contains(id.toUpperCase(Locale.ROOT))
                            ? pass(ID_IOS_APP, "app " + id + " listed in webcredentials.apps")
                            : fail(ID_IOS_APP, "app " + id + " not listed in webcredentials.apps"));
                }
                checks.add(drift(iosFacts(ExpectedAssociationFileBuilder.appleAppSiteAssociation(party)), found));
            }
        }
        checks.add(new CheckResult("content.ios.cdn", CheckStatus.WARN,
                "iOS reads this file through app-site-association.cdn-apple.com, which caches aggressively; "
                        + "a correct file may not take effect immediately"));
        return checks;
    }

    private static List<CheckResult> fingerprintChecks(AndroidApp app, List<AndroidApp> allApps, Set<String> found) {
        String pkg = app.getPackageName();
        List<CheckResult> checks = new ArrayList<>();
        if (ExpectedAssociationFileBuilder.fingerprints(app).isEmpty()) {
            checks.add(fail(ID_FINGERPRINT, "no fingerprint configured for " + PACKAGE + pkg));
        }
        for (String fingerprint : ExpectedAssociationFileBuilder.fingerprints(app)) {
            if (!found.contains("F|" + pkg + "|" + ExpectedAssociationFileBuilder.normalizeOrRaw(fingerprint))) {
                checks.add(fail(ID_FINGERPRINT,
                        "fingerprint " + fingerprint + " for package " + pkg + " not listed"));
            }
        }
        Set<String> configured = configuredFingerprints(pkg, allApps);
        String prefix = "F|" + pkg + "|";
        long unconfigured = found.stream().filter(f -> f.startsWith(prefix))
                .filter(f -> !configured.contains(f.substring(prefix.length()))).count();
        if (unconfigured > 0) {
            checks.add(new CheckResult(ID_FINGERPRINT, CheckStatus.WARN,
                    "file lists " + unconfigured + " fingerprint(s) for package " + pkg + " not in configuration"));
        }
        if (checks.isEmpty()) {
            checks.add(pass(ID_FINGERPRINT, "all configured fingerprints for package " + pkg + " listed"));
        }
        return checks;
    }

    /** Fingerprints configured on any entry for the same package are expected, not "unconfigured". */
    private static Set<String> configuredFingerprints(String pkg, List<AndroidApp> allApps) {
        Set<String> configured = new HashSet<>();
        for (AndroidApp other : allApps) {
            if (pkg.equals(other.getPackageName())) {
                for (String fingerprint : ExpectedAssociationFileBuilder.fingerprints(other)) {
                    configured.add(ExpectedAssociationFileBuilder.normalizeOrRaw(fingerprint));
                }
            }
        }
        return configured;
    }

    /** The relation must sit on a statement that also carries each configured fingerprint; an app with none cannot pass. */
    private static boolean relationBound(AndroidApp app, Set<String> found) {
        String pkg = app.getPackageName();
        if (ExpectedAssociationFileBuilder.fingerprints(app).isEmpty()) {
            return false;
        }
        for (String fingerprint : ExpectedAssociationFileBuilder.fingerprints(app)) {
            if (!found.contains("RF|" + pkg + "|" + ExpectedAssociationFileBuilder.normalizeOrRaw(fingerprint))) {
                return false;
            }
        }
        return true;
    }

    /** Drops each {@code F|} fact that has an {@code RF|} twin, so a fingerprint is counted once. */
    private static Set<String> withoutShadowedFingerprints(Set<String> facts) {
        Set<String> out = new HashSet<>(facts);
        out.removeIf(f -> f.startsWith("F|") && facts.contains("R" + f));
        return out;
    }

    private static CheckResult drift(Set<String> expectedFacts, Set<String> deployedFacts) {
        Set<String> expected = withoutShadowedFingerprints(expectedFacts);
        Set<String> deployed = withoutShadowedFingerprints(deployedFacts);
        long missing = expected.stream().filter(f -> !deployed.contains(f)).count();
        long extra = deployed.stream().filter(f -> !expected.contains(f)).count();
        if (missing == 0 && extra == 0) {
            return pass(ID_DRIFT, "deployed file matches what Jans would generate");
        }
        return new CheckResult(ID_DRIFT, CheckStatus.WARN, "deployed file differs from what Jans would "
                + "generate: " + missing + " expected entr" + (missing == 1 ? "y" : "ies") + " missing, " + extra
                + " unexpected");
    }

    /**
     * Flattens an assetlinks document into comparable facts: {@code P|pkg} (package listed), {@code F|pkg|FP}
     * (normalized fingerprint) and {@code R|pkg} (login credentials relation present) and {@code RF|pkg|FP} (fingerprint on a statement
     * that carries the relation).
     */
    private static Set<String> androidFacts(JsonNode statements) {
        Set<String> facts = new HashSet<>();
        for (JsonNode statement : statements) {
            JsonNode target = statement.path("target");
            if (!"android_app".equals(target.path("namespace").asText()) || !target.path("package_name").isTextual()) {
                continue;
            }
            String pkg = target.path("package_name").asText();
            boolean hasRelation = hasLoginCredsRelation(statement);
            facts.add("P|" + pkg);
            if (hasRelation) {
                facts.add("R|" + pkg);
            }
            addFingerprintFacts(facts, pkg, target, hasRelation);
        }
        return facts;
    }

    private static void addFingerprintFacts(Set<String> facts, String pkg, JsonNode target, boolean hasRelation) {
        for (JsonNode fingerprint : array(target.path("sha256_cert_fingerprints"))) {
            String normalized = fingerprint.isTextual()
                    ? ExpectedAssociationFileBuilder.normalizeFingerprint(fingerprint.asText()) : null;
            if (normalized != null) {
                facts.add("F|" + pkg + "|" + normalized);
                if (hasRelation) {
                    facts.add("RF|" + pkg + "|" + normalized);
                }
            }
        }
    }

    private static boolean hasLoginCredsRelation(JsonNode statement) {
        for (JsonNode relation : array(statement.path("relation"))) {
            if (LOGIN_CREDS_RELATION.equals(relation.asText())) {
                return true;
            }
        }
        return false;
    }

    /** The node itself when it is a JSON array, else nothing to iterate (objects would iterate their values). */
    private static Iterable<JsonNode> array(JsonNode node) {
        return node.isArray() ? node : List.of();
    }

    /** The upper-cased {@code webcredentials.apps} entries. */
    private static Set<String> iosFacts(JsonNode association) {
        Set<String> facts = new HashSet<>();
        JsonNode apps = association.path("webcredentials").path("apps");
        for (JsonNode app : array(apps)) {
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
            checks.add(fail(ID_PARSE, "not valid JSON"));
            return null;
        }
        if (node == null || node.isMissingNode()) {
            checks.add(fail(ID_PARSE, "not valid JSON"));
            return null;
        }
        if (expectArray ? !node.isArray() : !node.isObject()) {
            checks.add(fail(ID_PARSE, expectArray ? "not a JSON array of statements" : "not a JSON object"));
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
