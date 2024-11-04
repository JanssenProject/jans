package io.jans.casa.plugins.consent.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.util.List;

import io.jans.casa.misc.Utils;

@DataEntry
@ObjectClass("jansClntAuthz")
public class ClientAuthorization extends Entry {

    @AttributeName(name = "clnId")
    private String jansClntId;

    @AttributeName(name = "jansScope")
    private List<String> scopes;

    @AttributeName(name = "jansUsrId")
    private String userId;

    public String getJansClntId() {
        return jansClntId;
    }

    public List<String> getScopes() {
        return Utils.nonNullList(scopes);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setJansClntId(String v) {
        this.jansClntId = v;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

}
