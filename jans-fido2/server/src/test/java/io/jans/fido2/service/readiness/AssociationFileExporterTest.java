/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.model.conf.AndroidApp;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.IosApp;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.service.readiness.NativeReadinessService.NotFound;

@ExtendWith(MockitoExtension.class)
class AssociationFileExporterTest {

    private static final String FP = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99";

    @Mock
    private AppConfiguration appConfiguration;

    @InjectMocks
    private AssociationFileExporter exporter;

    private RequestedParty party;

    @BeforeEach
    void setUp() {
        party = new RequestedParty();
        party.setId("example.com");
        Fido2Configuration config = new Fido2Configuration();
        config.setRequestedParties(List.of(party));
        when(appConfiguration.getFido2Configuration()).thenReturn(config);
    }

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

    @Test
    void assetLinks_isTheBuilderOutput_forTheConfiguredAndroidApps() {
        party.setAndroidApps(List.of(android("com.example.app", FP.toLowerCase())));

        JsonNode file = exporter.assetLinks("example.com");

        assertEquals(ExpectedAssociationFileBuilder.assetLinks(party), file);
        assertEquals("com.example.app", file.get(0).path("target").path("package_name").asText());
        assertEquals(FP, file.get(0).path("target").path("sha256_cert_fingerprints").get(0).asText());
    }

    @Test
    void appleAppSiteAssociation_listsTeamIdDotBundleId() {
        party.setIosApps(List.of(ios("TEAM123456", "com.example.app")));

        JsonNode file = exporter.appleAppSiteAssociation("example.com");

        assertEquals("TEAM123456.com.example.app", file.path("webcredentials").path("apps").get(0).asText());
    }

    @Test
    void unknownRp_isNotFound() {
        assertThrows(NotFound.class, () -> exporter.assetLinks("other.example"));
        assertThrows(NotFound.class, () -> exporter.appleAppSiteAssociation(null));
    }

    @Test
    void rpWithoutAppsOfThePlatform_hasNothingToExport() {
        party.setAndroidApps(List.of(android("com.example.app", FP)));
        party.setIosApps(List.of(ios("TEAM123456", " ")));

        assertThrows(NotFound.class, () -> exporter.appleAppSiteAssociation("example.com"));

        party.setAndroidApps(null);
        assertThrows(NotFound.class, () -> exporter.assetLinks("example.com"));
    }
}
