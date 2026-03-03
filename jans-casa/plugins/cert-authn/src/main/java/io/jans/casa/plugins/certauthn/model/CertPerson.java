package io.jans.casa.plugins.certauthn.model;

import io.jans.casa.core.model.IdentityPerson;
import io.jans.orm.annotation.*;

import java.util.List;

@DataEntry
@ObjectClass("jansPerson")
public class CertPerson extends IdentityPerson {

    @AttributeName(name = "jans509Certificate")
    private List<String> x509Certificates;

    public List<String> getX509Certificates() {
        return x509Certificates;
    }

    public void setX509Certificates(List<String> x509Certificates) {
        this.x509Certificates = x509Certificates;
    }
    
}
