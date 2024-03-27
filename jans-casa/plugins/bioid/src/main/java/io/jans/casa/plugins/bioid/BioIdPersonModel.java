package io.jans.casa.plugins.bioid;

import java.util.Map;

import io.jans.casa.core.model.BasePerson;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass("jansPerson")
public class BioIdPersonModel extends BasePerson {

    @JsonObject
    @AttributeName(name = "jansCredential")
    private Map<String, Map<String, Object>> jansCredential;

    public Map<String, Map<String, Object>> getJansCredential() {
        return jansCredential;
    }

    public void setJansCredential(Map<String, Map<String, Object>> jansCredential) {
        this.jansCredential = jansCredential;
    }
}
