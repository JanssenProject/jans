package org.xdi.oxauth.uma.authorization;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class NeedInfoAuthenticationContext implements Serializable {

    @JsonProperty(value = "required_acr")
    private List<String> requiredAcrs;

    public NeedInfoAuthenticationContext() {
    }

    public List<String> getRequiredAcrs() {
        return requiredAcrs;
    }

    public void setRequiredAcrs(List<String> requiredAcrs) {
        this.requiredAcrs = requiredAcrs;
    }
}
