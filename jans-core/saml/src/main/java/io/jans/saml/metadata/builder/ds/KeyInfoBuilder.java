package io.jans.saml.metadata.builder.ds;

import io.jans.saml.metadata.model.ds.KeyInfo;
import io.jans.saml.metadata.model.ds.X509Data;

public class KeyInfoBuilder {

    private KeyInfo keyInfo;

    public KeyInfoBuilder(final KeyInfo keyInfo) {

        this.keyInfo = keyInfo;
    }

    public KeyInfoBuilder id(final String id) {

        this.keyInfo.setId(id);
        return this;
    }

    public X509DataBuilder x509Data() {

        X509Data data = new X509Data();
        this.keyInfo.addData(data);
        return new X509DataBuilder(data);
    }
}