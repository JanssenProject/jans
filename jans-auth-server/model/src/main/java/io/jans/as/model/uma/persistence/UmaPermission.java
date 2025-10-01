/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma.persistence;

import com.google.common.collect.Maps;
import io.jans.as.model.util.Pair;
import io.jans.as.model.util.Util;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UMA permission
 *
 * @author Yuriy Zabrovarnyy
 * @version 2.0, date: 17/05/2017
 */
@DataEntry
@ObjectClass(value = "jansUmaResourcePermission")
public class UmaPermission implements Serializable {

    public static final String PCT = "pct";

    @DN
    private String dn;
    @AttributeName(name = "jansStatus")
    private String status;
    @AttributeName(name = "jansTicket", consistency = true)
    private String ticket;
    @AttributeName(name = "jansConfCode")
    private String configurationCode;
    @AttributeName(name = "exp")
    private Date expirationDate;
    @AttributeName(name = "del")
    private boolean deletable = true;

    @AttributeName(name = "jansResourceSetId")
    private String resourceId;
    @AttributeName(name = "jansUmaScope")
    private List<String> scopeDns;

    @JsonObject
    @AttributeName(name = "jansAttrs")
    private Map<String, String> attributes;

    @Expiration
    private Integer ttl;

    private boolean expired;

    public UmaPermission() {
    }

    public UmaPermission(String resourceId, List<String> scopes, String ticket,
                         String configurationCode, Pair<Date, Integer> expirationDate) {
        this.resourceId = resourceId;
        this.scopeDns = scopes;
        this.ticket = ticket;
        this.configurationCode = configurationCode;
        this.expirationDate = expirationDate.getFirst();
        this.ttl = expirationDate.getSecond();

        checkExpired();
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public void resetTtlFromExpirationDate() {
        final Long duration = Duration.between(new Date().toInstant(), getExpirationDate().toInstant()).getSeconds();
        final Integer calculatedTtl = duration.intValue();
        if (calculatedTtl != null) {
            setTtl(calculatedTtl);
        }
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public void checkExpired() {
        checkExpired(new Date());
    }

    public void checkExpired(Date now) {
        if (now.after(expirationDate) && deletable) {
            expired = true;
        }
    }

    public boolean isValid() {
        return !expired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfigurationCode() {
        return configurationCode;
    }

    public void setConfigurationCode(String configurationCode) {
        this.configurationCode = configurationCode;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public List<String> getScopeDns() {
        if (scopeDns == null) {
            scopeDns = new ArrayList<>();
        }
        return scopeDns;
    }

    public void setScopeDns(List<String> scopeDns) {
        this.scopeDns = scopeDns;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = Maps.newHashMap();
        }
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UmaPermission that = (UmaPermission) o;

        return !(ticket != null ? !ticket.equals(that.ticket) : that.ticket != null);

    }

    @Override
    public int hashCode() {
        return ticket != null ? ticket.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UmaPermission{" +
                "dn='" + dn + '\'' +
                ", status='" + status + '\'' +
                ", ticket='" + ticket + '\'' +
                ", configurationCode='" + configurationCode + '\'' +
                ", expirationDate=" + expirationDate +
                ", resourceId='" + resourceId + '\'' +
                ", scopeDns=" + scopeDns +
                ", expired=" + expired +
                '}';
    }
}
