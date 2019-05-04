package org.gluu.oxauth.uma.authorization;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.gluu.oxauth.model.common.AbstractToken;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.JwtClaims;
import org.gluu.oxauth.uma.service.UmaPctService;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuriyz on 05/30/2017.
 */
@DataEntry
@ObjectClass(values = {"top", "oxAuthUmaPCT"})
public class UmaPCT extends AbstractToken {

    private final static Logger log = LoggerFactory.getLogger(UmaPCT.class);

    @DN
    private String dn;
    @AttributeName(name = "oxAuthClientId")
    private String clientId;
    @AttributeName(name = "oxClaimValues")
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
