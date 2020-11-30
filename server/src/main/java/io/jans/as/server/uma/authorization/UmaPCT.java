/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author yuriyz on 05/30/2017.
 */
@DataEntry
@ObjectClass(value = "jansUmaPCT")
public class UmaPCT extends AbstractToken {

    private final static Logger log = LoggerFactory.getLogger(UmaPCT.class);

    @DN
    private String dn;
    @AttributeName(name = "clnId")
    private String clientId;
    @AttributeName(name = "jansClaimValues")
    private String claimValuesAsJson;

    public UmaPCT() {
        super(UmaPctService.DEFAULT_PCT_LIFETIME);
    }

    public UmaPCT(int lifeTime) {
        super(lifeTime);
    }

    protected UmaPCT(String code, Date creationDate, Date expirationDate) {
        super(code, creationDate, expirationDate);
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClaimValuesAsJson() {
        return claimValuesAsJson;
    }

    public void setClaimValuesAsJson(String claimValuesAsJson) {
        this.claimValuesAsJson = claimValuesAsJson;
    }

    public JwtClaims getClaims() {
        try {
            return StringUtils.isNotBlank(claimValuesAsJson) ? new JwtClaims(new JSONObject(claimValuesAsJson)) : new JwtClaims();
        } catch (Exception e) {
            log.error("Failed to parse PCT claims. " + e.getMessage(), e);
            return null;
        }
    }

    public void setClaims(JwtClaims claims) throws InvalidJwtException {
        if (claims != null) {
            claimValuesAsJson = claims.toJsonString();
        } else {
            claimValuesAsJson = null;
        }
    }
}
