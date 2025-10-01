package io.jans.saml.metadata.builder.ds;

import java.util.List;
import io.jans.saml.metadata.model.ds.X509Data;

public class X509DataBuilder  {

    private final X509Data data;

    public X509DataBuilder(final X509Data data) {

        this.data = data;
    }

    public X509DataBuilder x509Certificate(final String data) {

        this.data.addX509Certificate(data);
        return this;
    }

    public X509DataBuilder x509Certificates(final List<String> datalist) {

        this.data.setX509Certificates(datalist);
        return this;
    }
}