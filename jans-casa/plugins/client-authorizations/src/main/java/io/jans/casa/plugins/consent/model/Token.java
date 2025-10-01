package io.jans.casa.plugins.consent.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass("jansToken")
public class Token extends Entry {

    @AttributeName(name = "tknCde")
    private String tokenCode;

    @AttributeName(name = "clnId")
    private String clientId;

    @AttributeName(name = "tknTyp")
    private String tokenType;

    @AttributeName(name = "usrId")
    private String userId;

    public String getTokenCode() {
        return tokenCode;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getUserId() {
        return userId;
    }

    public void setTokenCode(String tokenCode) {
        this.tokenCode = tokenCode;
    }

    public void setClientId(String v) {
        this.clientId = v;
    }

    public void setTokenType(String v) {
        this.tokenType = v;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
