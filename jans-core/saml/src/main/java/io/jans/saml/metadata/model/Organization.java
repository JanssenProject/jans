package io.jans.saml.metadata.model;

import java.util.ArrayList;
import java.util.List;

public class Organization {

    private List<LocalizedText> names;
    private List<LocalizedText> displayNames;
    private List<LocalizedText> urls;

    public Organization() {

        this.names = new ArrayList<>();
        this.displayNames = new ArrayList<>();
        this.urls = new ArrayList<>();
    }

    public List<LocalizedText> getNames() {

        return this.names;
    }

    public void addName(LocalizedText name) {

        this.names.add(name);
    }

    public List<LocalizedText> getDisplayNames() {

        return this.displayNames;
    }

    public void addDisplayName(final LocalizedText displayName) {

        this.displayNames.add(displayName);
    }

    public List<LocalizedText> getUrls() {

        return this.urls;
    }

    public void addUrl(final LocalizedText url) {

        this.urls.add(url);
    }
}