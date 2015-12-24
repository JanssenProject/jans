package org.xdi.oxd.rs.protect;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/12/2015
 */

public class RsResource implements Serializable {

    @JsonProperty(value = "id")
    String id;
    @JsonProperty(value = "scopes")
    List<String> scopes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
