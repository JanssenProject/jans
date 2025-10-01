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

    public String[] getExternalUid() {
        return externalUid;
    }

    public void setExternalUid(String[] externalUid) {
        this.externalUid = externalUid;
    }

    public UserAuthenticatorList getAuthenticatorList() {
        return authenticatorList;
    }

    public void setAuthenticatorList(UserAuthenticatorList authenticatorList) {
        this.authenticatorList = authenticatorList;
    }
}
