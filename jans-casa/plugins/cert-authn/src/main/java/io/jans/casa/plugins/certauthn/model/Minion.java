package io.jans.casa.plugins.certauthn.model;

import io.jans.casa.core.model.BasePerson ;
import io.jans.orm.annotation.*;

@DataEntry
@ObjectClass("jansPerson")
public class Minion extends BasePerson {
    
    @AttributeName(name = "jansPreferredMethod")
    private String preferredMethod;

    public String getPreferredMethod() {
        return preferredMethod;
    }

    public void setPreferredMethod(String preferredMethod) {
        this.preferredMethod = preferredMethod;
    }
    
}
