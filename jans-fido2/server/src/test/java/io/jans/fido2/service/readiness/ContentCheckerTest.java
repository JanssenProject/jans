/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.jans.fido2.model.conf.AndroidApp;
import io.jans.fido2.model.conf.IosApp;
import io.jans.fido2.model.conf.RequestedParty;

class ContentCheckerTest {

    private static final String FP1 = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99";
    private static final String FP2 = "11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00";
    private static final String REL = "delegate_permission/common.get_login_creds";
    private static final String MARKER = "SECRET-MARKER-7c1e";

    private final ContentChecker checker = new ContentChecker();

    private static AndroidApp android(String pkg, String... fingerprints) {
        AndroidApp app = new AndroidApp();
        app.setPackageName(pkg);
        app.setSha256CertFingerprints(List.of(fingerprints));
        return app;
    }

    private static IosApp ios(String team, String bundle) {
        IosApp app = new IosApp();
        app.setTeamId(team);
        app.setBundleId(bundle);
        return app;
    }

    private static RequestedParty party(List<AndroidApp> androidApps, List<IosApp> iosApps) {
        RequestedParty party = new RequestedParty();
        party.setAndroidApps(androidApps);
        party.setIosApps(iosApps);
        return party;
    }

    private static String statement(String relation, String pkg, String... fingerprints) {
        String fps = java.util.Arrays.stream(fingerprints).map(f -> "\"" + f + "\"").collect(Collectors.joining(","));
        return "{\"relation\":[\"" + relation + "\"],\"target\":{\"namespace\":\"android_app\","
                + "\"package_name\":\"" + pkg + "\",\"sha256_cert_fingerprints\":[" + fps + "]}}";
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private List<CheckResult> android(RequestedParty party, String json) {
        return checker.checkAssetLinks(party, bytes(json));
    }

    private static List<CheckResult> byId(List<CheckResult> checks, String id) {
        return checks.stream().filter(c -> c.id().equals(id)).collect(Collectors.toList());
    }

    private static boolean any(List<CheckResult> checks, String id, CheckStatus status, String text) {
        return byId(checks, id).stream().anyMatch(c -> c.status() == status && c.reason().contains(text));
    }

    private static void assertNone(List<CheckResult> checks, CheckStatus status) {
        assertTrue(checks.stream().noneMatch(c -> c.status() == status), checks.toString());
    }

    private static final RequestedParty ONE_APP = party(List.of(android("com.example.app", FP1)), List.of());

    // ---- Android

    @Test
    void androidFile_fullMatch_passesEverythingAndDoesNotDrift() {
        List<CheckResult> checks = android(ONE_APP, "[" + statement(REL, "com.example.app", FP1) + "]");
        assertNone(checks, CheckStatus.FAIL);
        assertNone(checks, CheckStatus.WARN);
        assertEquals(CheckStatus.PASS, byId(checks, "content.drift").get(0).status());
    }

    @Test
    void androidFile_wrongPackage_failsNamingTheConfiguredPackage() {
        List<CheckResult> checks = android(ONE_APP, "[" + statement(REL, "com.other.app", FP1) + "]");
        assertTrue(any(checks, "content.android.package", CheckStatus.FAIL, "com.example.app not listed"));
        assertFalse(checks.toString().contains("com.other.app"));
    }

    @Test
    void androidFile_missingFingerprint_failsNamingTheConfiguredOne() {
        RequestedParty party = party(List.of(android("com.example.app", FP1, FP2)), List.of());
        List<CheckResult> checks = android(party, "[" + statement(REL, "com.example.app", FP1) + "]");
        assertTrue(any(checks, "content.android.fingerprint", CheckStatus.FAIL, FP2));
        assertFalse(any(checks, "content.android.fingerprint", CheckStatus.FAIL, FP1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99",
            "AABBCCDDEEFF00112233445566778899AABBCCDDEEFF00112233445566778899" })
    void androidFile_fingerprintCaseAndColonVariation_stillMatches(String variant) {
        List<CheckResult> checks = android(ONE_APP, "[" + statement(REL, "com.example.app", variant) + "]");
        assertNone(checks, CheckStatus.FAIL);
        assertNone(checks, CheckStatus.WARN);
    }

    @Test
    void androidFile_extraFingerprint_warnsWithCountOnly() {
        List<CheckResult> checks = android(ONE_APP, "[" + statement(REL, "com.example.app", FP1, FP2) + "]");
        assertNone(checks, CheckStatus.FAIL);
        assertTrue(any(checks, "content.android.fingerprint", CheckStatus.WARN, "1 fingerprint(s)"));
        assertFalse(checks.toString().contains("11:22:33"));
    }

    @Test
    void androidFile_missingRelation_fails() {
        List<CheckResult> checks = android(ONE_APP,
                "[" + statement("delegate_permission/common.handle_all_urls", "com.example.app", FP1) + "]");
        assertTrue(any(checks, "content.android.relation", CheckStatus.FAIL, "com.example.app"));
    }

    @Test
    void androidFile_multipleApps_eachCheckedSeparately() {
        RequestedParty party = party(List.of(android("com.a", FP1), android("com.b", FP2)), List.of());
        List<CheckResult> checks = android(party,
                "[" + statement(REL, "com.a", FP1) + "," + statement(REL, "com.b", FP1) + "]");
        assertTrue(any(checks, "content.android.package", CheckStatus.PASS, "com.a"));
        assertTrue(any(checks, "content.android.package", CheckStatus.PASS, "com.b"));
        assertTrue(any(checks, "content.android.fingerprint", CheckStatus.FAIL, FP2));
        assertFalse(any(checks, "content.android.fingerprint", CheckStatus.FAIL, FP1));
    }

    @Test
    void androidFile_noAppsConfigured_isAPassWithANote() {
        List<CheckResult> checks = android(party(List.of(), List.of()), "[]");
        assertEquals(1, checks.size());
        assertEquals(CheckStatus.PASS, checks.get(0).status());
        assertTrue(checks.get(0).reason().contains("no Android apps configured"));
    }

    // ---- iOS

    private List<CheckResult> aasa(RequestedParty party, String json) {
        return checker.checkAppleAppSiteAssociation(party, bytes(json));
    }

    private static final RequestedParty ONE_IOS = party(List.of(), List.of(ios("ABCDE12345", "com.example.app")));

    @Test
    void aasa_fullMatch_passesAndDoesNotDrift() {
        List<CheckResult> checks = aasa(ONE_IOS, "{\"webcredentials\":{\"apps\":[\"ABCDE12345.com.example.app\"]}}");
        assertNone(checks, CheckStatus.FAIL);
        assertEquals(CheckStatus.PASS, byId(checks, "content.drift").get(0).status());
    }

    @Test
    void aasa_wrongTeamIdOrBundleId_failsNamingTheConfiguredValue() {
        assertTrue(any(aasa(ONE_IOS, "{\"webcredentials\":{\"apps\":[\"ZZZZZ99999.com.example.app\"]}}"),
                "content.ios.app", CheckStatus.FAIL, "ABCDE12345.com.example.app"));
        assertTrue(any(aasa(ONE_IOS, "{\"webcredentials\":{\"apps\":[\"ABCDE12345.com.example.other\"]}}"),
                "content.ios.app", CheckStatus.FAIL, "ABCDE12345.com.example.app"));
    }

    @Test
    void aasa_multipleApps_eachChecked() {
        RequestedParty party = party(List.of(), List.of(ios("ABCDE12345", "com.a"), ios("ABCDE12345", "com.b")));
        List<CheckResult> checks = aasa(party, "{\"webcredentials\":{\"apps\":[\"ABCDE12345.com.a\"]}}");
        assertTrue(any(checks, "content.ios.app", CheckStatus.PASS, "ABCDE12345.com.a"));
        assertTrue(any(checks, "content.ios.app", CheckStatus.FAIL, "ABCDE12345.com.b"));
    }

    @Test
    void aasa_noAppsConfigured_isAPassWithANote() {
        List<CheckResult> checks = aasa(party(List.of(), List.of()), "{}");
        assertTrue(any(checks, "content.ios.app", CheckStatus.PASS, "no iOS apps configured"));
    }

    @Test
    void cdnWarning_isPresentWhetherIosPassesOrFails_andNeverAFail() {
        List<CheckResult> passing = aasa(ONE_IOS, "{\"webcredentials\":{\"apps\":[\"ABCDE12345.com.example.app\"]}}");
        List<CheckResult> failing = aasa(ONE_IOS, "{\"webcredentials\":{\"apps\":[]}}");
        List<CheckResult> unparseable = aasa(ONE_IOS, "not json");
        for (List<CheckResult> checks : List.of(passing, failing, unparseable)) {
            assertEquals(1, byId(checks, "content.ios.cdn").size());
            assertEquals(CheckStatus.WARN, byId(checks, "content.ios.cdn").get(0).status());
        }
        assertEquals(CheckStatus.PASS, byId(passing, "content.ios.app").get(0).status());
    }

    // ---- unusable bodies

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "{not json", "[1,2", "\u0000\u0001binary", "{}", "\"a string\"", "42" })
    void unusableAssetLinks_failsWithAFixedReason(String body) {
        List<CheckResult> checks = android(ONE_APP, body);
        assertEquals(1, checks.size());
        assertEquals("content.parse", checks.get(0).id());
        assertEquals(CheckStatus.FAIL, checks.get(0).status());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "{not json", "[]", "\"x\"" })
    void unusableAasa_failsWithAFixedReason(String body) {
        List<CheckResult> checks = byId(aasa(ONE_IOS, body), "content.parse");
        assertEquals(1, checks.size());
        assertEquals(CheckStatus.FAIL, checks.get(0).status());
    }

    @Test
    void deeplyNestedJson_failsWithoutLeakingParserText() {
        String deep = "[".repeat(5000) + "]".repeat(5000);
        List<CheckResult> checks = android(ONE_APP, deep);
        assertEquals(1, checks.size());
        assertEquals(CheckStatus.FAIL, checks.get(0).status());
        assertEquals("not valid JSON", checks.get(0).reason());
    }

    @Test
    void noResultContainsBodyBytesOrParserText() {
        String malformed = "{\"" + MARKER + "\": [ oops";
        String valid = "[{\"relation\":[\"" + MARKER + "\"],\"target\":{\"namespace\":\"android_app\","
                + "\"package_name\":\"" + MARKER + "\",\"sha256_cert_fingerprints\":[\"" + MARKER + "\"]}}]";
        String validAasa = "{\"webcredentials\":{\"apps\":[\"" + MARKER + "\"]},\"" + MARKER + "\":1}";
        for (List<CheckResult> checks : List.of(android(ONE_APP, malformed), android(ONE_APP, valid),
                aasa(ONE_IOS, malformed), aasa(ONE_IOS, validAasa))) {
            assertFalse(checks.toString().contains(MARKER), checks.toString());
            assertFalse(checks.toString().contains("Unexpected"), checks.toString());
            assertFalse(checks.toString().contains("line:"), checks.toString());
        }
    }

    // ---- drift

    @Test
    void drift_ignoresKeyOrderWhitespaceAndUnrelatedContent() {
        String reordered = "[ { \"target\" : { \"sha256_cert_fingerprints\" : [ \"" + FP1 + "\" ], "
                + "\"package_name\" : \"com.example.app\", \"namespace\" : \"android_app\" }, "
                + "\"relation\" : [ \"" + REL + "\" ] },\n"
                + "{\"relation\":[\"delegate_permission/common.handle_all_urls\"],\"target\":{\"namespace\":\"web\","
                + "\"site\":\"https://example.com\"}} ]";
        assertEquals(CheckStatus.PASS, byId(android(ONE_APP, reordered), "content.drift").get(0).status());

        String aasaWithApplinks = "{\"applinks\":{\"details\":[]},\"webcredentials\":{\"apps\":"
                + "[\"abcde12345.com.example.app\"]}}";
        assertEquals(CheckStatus.PASS, byId(aasa(ONE_IOS, aasaWithApplinks), "content.drift").get(0).status());
    }

    @Test
    void drift_rotatedFingerprint_isAWarnNotAFail_withCounts() {
        String rotated = "[" + statement(REL, "com.example.app", FP2) + "]";
        List<CheckResult> drift = byId(android(ONE_APP, rotated), "content.drift");
        assertEquals(CheckStatus.WARN, drift.get(0).status());
        assertTrue(drift.get(0).reason().contains("1 expected entry missing, 1 unexpected"), drift.get(0).reason());
    }

    @Test
    void drift_isSkippedWhenNothingIsConfigured() {
        assertTrue(byId(android(party(List.of(), List.of()), "[]"), "content.drift").isEmpty());
        assertTrue(byId(aasa(party(List.of(), List.of()), "{}"), "content.drift").isEmpty());
    }

    // ---- expected-file builder

    @Test
    void builder_buildsOneStatementPerAppWithNormalizedFingerprints() {
        RequestedParty party = party(List.of(android("com.a", "aabbcc", FP1), android("com.b", FP2)),
                List.of(ios("T1", "b.one")));
        var assetLinks = ExpectedAssociationFileBuilder.assetLinks(party);
        assertEquals(2, assetLinks.size());
        assertEquals(REL, assetLinks.get(0).path("relation").get(0).asText());
        assertEquals("AA:BB:CC", assetLinks.get(0).path("target").path("sha256_cert_fingerprints").get(0).asText());
        assertEquals(FP1, assetLinks.get(0).path("target").path("sha256_cert_fingerprints").get(1).asText());
        assertEquals("T1.b.one", ExpectedAssociationFileBuilder.appleAppSiteAssociation(party)
                .path("webcredentials").path("apps").get(0).asText());
        assertNotEquals(assetLinks.get(0), assetLinks.get(1));
    }

    // ---- review hardening

    @Test
    void relationOnADifferentStatementThanTheFingerprint_doesNotPass() {
        String split = "[" + statement("delegate_permission/common.handle_all_urls", "com.example.app", FP1) + ","
                + statement(REL, "com.example.app", FP2) + "]";
        assertTrue(any(android(ONE_APP, split), "content.android.relation", CheckStatus.FAIL, "com.example.app"));
    }

    @Test
    void trailingJsonAfterTheDocument_failsParse() {
        String body = "[" + statement(REL, "com.example.app", FP1) + "] []";
        assertEquals(CheckStatus.FAIL, android(ONE_APP, body).get(0).status());
        assertEquals("content.parse", android(ONE_APP, body).get(0).id());
    }

    @Test
    void appsAsObject_isNotAcceptedAsAnArray() {
        String body = "{\"webcredentials\":{\"apps\":{\"x\":\"ABCDE12345.com.example.app\"}}}";
        assertTrue(any(aasa(ONE_IOS, body), "content.ios.app", CheckStatus.FAIL, "not listed"));
    }

    @Test
    void fingerprintWithInvalidCharacters_isNotNormalizedToAMatch() {
        assertNull(ExpectedAssociationFileBuilder.normalizeFingerprint(FP1 + "Z"));
        String body = "[" + statement(REL, "com.example.app", FP1 + "Z") + "]";
        assertTrue(any(android(ONE_APP, body), "content.android.fingerprint", CheckStatus.FAIL, "not listed"));
    }

    @Test
    void relationAndFingerprintsAsObjects_doNotEstablishAMatch() {
        String body = "[{\"relation\":{\"r\":\"" + REL + "\"},\"target\":{\"namespace\":\"android_app\","
                + "\"package_name\":\"com.example.app\",\"sha256_cert_fingerprints\":{\"f\":\"" + FP1 + "\"}}}]";
        List<CheckResult> checks = android(ONE_APP, body);
        assertTrue(any(checks, "content.android.relation", CheckStatus.FAIL, "com.example.app"));
        assertTrue(any(checks, "content.android.fingerprint", CheckStatus.FAIL, "not listed"));
    }
}
