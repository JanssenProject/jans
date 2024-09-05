package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.LocalizedText;

public class LocalizedTextBuilder {

    private LocalizedText localizedText;

    public LocalizedTextBuilder(final LocalizedText localizedText) {

        this.localizedText = localizedText;
    }

    public LocalizedTextBuilder language(final String language) {

        this.localizedText.setLanguage(language);
        return this;
    }

    public LocalizedTextBuilder text(final String text) {

        this.localizedText.setText(text);
        return this;
    }
}