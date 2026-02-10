package io.jans.casa.plugins.injiwallet.model;

import io.jans.casa.core.model.IdentityPerson;
import io.jans.orm.annotation.*;

@DataEntry
@ObjectClass("jansPerson")
public class PersonWithCredentials extends IdentityPerson {

    @AttributeName(name = "verifiableCredentials")
    private String verifiableCredentials;

    public String getVerifiableCredentials() {
        return verifiableCredentials;
    }
    
    public void setVerifiableCredentials(String verifiableCredentials) {
        this.verifiableCredentials = verifiableCredentials;
    }

}
