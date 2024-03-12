package io.jans.saml.metadata.model;

public enum SAMLBinding {
    SAML2_HTTP_REDIRECT("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"),
    SAML2_HTTP_POST("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");

    private final String value;

    private SAMLBinding(final String value) {
        this.value = value;
    }

    public String getValue() {

        return this.value;
    }
}
