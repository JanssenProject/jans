/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.service.readiness.NativeReadinessService.NotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Produces the {@code assetlinks.json} and {@code apple-app-site-association} files of a configured relying
 * party, for the admin to deploy on the RP-ID host. Both come from {@link ExpectedAssociationFileBuilder},
 * the same definition the readiness checker compares the deployed files against. Nothing is fetched or served
 * from here.
 */
@ApplicationScoped
public class AssociationFileExporter {

    @Inject
    private AppConfiguration appConfiguration;

    /**
     * @throws NotFound when no relying party has that id, or it has no Android app to describe
     */
    public JsonNode assetLinks(String rpConfigId) {
        RequestedParty party = party(rpConfigId);
        if (ExpectedAssociationFileBuilder.androidApps(party).isEmpty()) {
            throw new NotFound();
        }
        return ExpectedAssociationFileBuilder.assetLinks(party);
    }

    /**
     * @throws NotFound when no relying party has that id, or it has no complete iOS app to describe
     */
    public JsonNode appleAppSiteAssociation(String rpConfigId) {
        RequestedParty party = party(rpConfigId);
        JsonNode file = ExpectedAssociationFileBuilder.appleAppSiteAssociation(party);
        if (file.path("webcredentials").path("apps").isEmpty()) {
            throw new NotFound();
        }
        return file;
    }

    private RequestedParty party(String rpConfigId) {
        Fido2Configuration config = appConfiguration.getFido2Configuration();
        RequestedParty party = config == null ? null : NativeReadinessService.find(config, rpConfigId);
        if (party == null) {
            throw new NotFound();
        }
        return party;
    }
}
