package io.jans.saml.metadata.model;

public class LocalizedText {

    private String language;
    private String text;
    
    public LocalizedText() {

        this.language = language;
        this.text = text;
    }

    public String getLanguage() {

        return this.language;
    }

    public void setLanguage(final String language) {

        this.language = language;
    }

    public String getText() {

        return this.text;
    }

    public void setText(final String text) {

        this.text = text;
    }
}