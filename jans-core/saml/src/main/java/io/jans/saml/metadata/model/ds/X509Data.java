package io.jans.saml.metadata.model.ds;

import java.util.ArrayList;
import java.util.List;

public class X509Data {

    private List<String> x509certificates;

    public X509Data() {

        this.x509certificates = new ArrayList<String>();
    }

    public List<String> getX509Certificates() {

        return this.x509certificates;
    }

    public String getFirstX509Certificate() {

        if(!x509certificates.isEmpty()) {
            return x509certificates.get(0);
        }
        return null;
    }

    public void addX509Certificate(final String x509certificate) {

        this.x509certificates.add(x509certificate);
    }

    public void setX509Certificates(final List<String> x509certificates) {

        this.x509certificates = x509certificates;
    }
}

