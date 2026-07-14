package io.jans.shibboleth.trust.config.metadata.manual;

import java.util.Objects;

public enum SamlBinding {
    
    HTTP_POST("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
    HTTP_REDIRECT("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
    
    private final String uri;

    private SamlBinding(String uri) {
        this.uri = uri;
    }

    public String uri() {
        
        return uri;
    }
}
