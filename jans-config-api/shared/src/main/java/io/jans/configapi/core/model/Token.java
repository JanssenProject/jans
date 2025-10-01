package io.jans.configapi.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass("jansToken")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Token extends Entry {

    private static final long serialVersionUID = 1L;

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

    @Override
    public String toString() {
        return "Token [tokenCode=" + tokenCode + ", clientId=" + clientId + ", tokenType=" + tokenType + ", userId="
                + userId + "]";
    }

}
