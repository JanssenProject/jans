package io.jans.saml.metadata.model;

public enum SAMLBinding {
    SOAP("urn:oasis:names:tc:SAML:2.0:bindings:SOAP"),
    REVERSE_SOAP("urn:oasis:names:tc:SAML:2.0:bindings:PAOS"),
    HTTP_REDIRECT("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"),
    HTTP_POST("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
    HTTP_ARTIFACT("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"),
    URI("urn:oasis:names:tc:SAML:2.0:bindings:URI"),
    UNKNOWN("");

    private final String value;

    private SAMLBinding(final String value) {
        this.value = value;
    }

    public String value() {

        return this.value;
    }

    public static SAMLBinding fromString(final String value) {

        for(SAMLBinding binding: SAMLBinding.values()) {
            if(binding.value.equals(value)) {
                return binding;
            }
        }
        return UNKNOWN;
    }
}
