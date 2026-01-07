package io.jans.configapi.core.model.adminui;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import java.util.Date;

@DataEntry
@ObjectClass(value = "adminUISession")
public class AdminUISession {
    @DN
    private String dn;
    @AttributeName(
            ignoreDuringUpdate = true
    )
    private String inum;
    @AttributeName(name = "sid")
    private String sessionId;
    @AttributeName(name = "jansUjwt")
    private String ujwt;
    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();
    @AttributeName(name = "exp")
    private Date expirationDate;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUjwt() {
        return ujwt;
    }

    public void setUjwt(String ujwt) {
        this.ujwt = ujwt;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}
