package io.jans.casa.plugins.bioid;

import java.util.Map;

import io.jans.casa.core.model.BasePerson;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.model.user.authenticator.UserAuthenticatorList;

@DataEntry
@ObjectClass("jansPerson")
public class BioIdPersonModel extends BasePerson {

    @AttributeName(name = "jansExtUid")
    private String[] externalUid;

    @AttributeName(name = "jansAuthenticator")
    @JsonObject
    private UserAuthenticatorList authenticatorList;

    @JsonObject
    @AttributeName(name = "jansCredential")
    private Map<String, Map<String, Object>> jansCredential;

    public Map<String, Map<String, Object>> getJansCredential() {
        return jansCredential;
    }

    public void setJansCredential(Map<String, Map<String, Object>> jansCredential) {
        this.jansCredential = jansCredential;
    }

    public String[] getExternalUid() {
        return externalUid;
    }

    public void setExternalUid(String[] externalUid) {
        this.externalUid = externalUid;
    }

    public UserAuthenticatorList getAuthenticator() {
        return authenticatorList;
    }

    public void setAuthenticator(UserAuthenticatorList authenticator) {
        this.authenticatorList = authenticator;
    }
}
