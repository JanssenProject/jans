package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.LocalizedText;
import io.jans.saml.metadata.model.Organization;

public class OrganizationBuilder {

    private Organization organization;

    public OrganizationBuilder(final Organization organization) {

        this.organization = organization;
    }

    public LocalizedTextBuilder name() {

        LocalizedText name = new LocalizedText();
        this.organization.addName(name);
        return new LocalizedTextBuilder(name);
    }

    public LocalizedTextBuilder diplayName() {

        LocalizedText displayName = new LocalizedText();
        this.organization.addDisplayName(displayName);
        return new LocalizedTextBuilder(displayName);
    }

    public LocalizedTextBuilder url() {

        LocalizedText url = new LocalizedText();
        this.organization.addUrl(url);
        return new LocalizedTextBuilder(url);
    }
}