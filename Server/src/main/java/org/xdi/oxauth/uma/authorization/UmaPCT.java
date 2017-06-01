package org.xdi.oxauth.uma.authorization;

import com.ocpsoft.pretty.faces.util.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.oxauth.model.common.AbstractToken;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaims;

import java.util.Date;

/**
 * @author yuriyz on 05/30/2017.
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthUmaPCT"})
public class UmaPCT extends AbstractToken {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "oxAuthClientId")
    private String clientId;
    @LdapAttribute(name = "oxClaimValues")
    private String claimValuesAsJson;

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

    public JwtClaims getClaims() throws JSONException {
        return StringUtils.isNotBlank(claimValuesAsJson) ? new JwtClaims(new JSONObject(claimValuesAsJson)) : new JwtClaims();
    }

    public void setClaims(JwtClaims claims) throws InvalidJwtException {
        if (claims != null) {
            claimValuesAsJson = claims.toJsonString();
        } else {
            claimValuesAsJson = null;
        }
    }
}
