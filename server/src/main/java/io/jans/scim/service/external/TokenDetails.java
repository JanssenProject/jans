package io.jans.scim.service.external;

import io.jans.orm.annotation.*;
import io.jans.orm.model.base.Entry;

import java.util.Date;

@DataEntry
@ObjectClass(value = "jansToken")
public class TokenDetails extends Entry {
	
    @AttributeName(name = "clnId")
    private String clientId;
    
    @AttributeName(name = "iat")
    private Date creationDate;
    
    @AttributeName(name = "exp")
    private Date expirationDate;
    
    @AttributeName(name = "tknTyp")
    private String tokenType;
    
    @AttributeName(name = "scp")
    private String scope;
    
    private String value;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date p_creationDate) {
        creationDate = p_creationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date p_expirationDate) {
        expirationDate = p_expirationDate;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

}
