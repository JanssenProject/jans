package org.xdi.oxauth.service.uma.authorization;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.ClaimMappingConfiguration;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */

public class AuthorizationContext {

    private static final Log LOG = Logging.getLog(AuthorizationContext.class);

    private final LdapEntryManager ldapEntryManager = ServerUtil.getLdapManager();

    private final UmaRPT m_rpt;
    private final ResourceSetPermission m_permission;
    private final IAuthorizationGrant m_grant;
    private final HttpServletRequest m_httpRequest;
    private final Map<String, List<String>> m_claims;

    public AuthorizationContext(UmaRPT p_rpt, ResourceSetPermission p_permission, IAuthorizationGrant p_grant,
                                HttpServletRequest p_httpRequest, Map<String, List<String>> p_claims) {
        m_rpt = p_rpt;
        m_permission = p_permission;
        m_grant = p_grant;
        m_httpRequest = p_httpRequest;
        m_claims = p_claims;
    }

    public Log getLog() {
        return LOG;
    }

    public HttpServletRequest getHttpRequest() {
        return m_httpRequest;
    }

    public List<String> getRequestClaim(String p_claimName) {
        if (StringUtils.isNotBlank(p_claimName) && m_claims != null) {
            final List<String> value = m_claims.get(p_claimName);
            if (value != null) {
                return Collections.unmodifiableList(value);
            }
        }
        return Collections.emptyList();
    }

    public String getIpAddress() {
        return m_httpRequest != null ? m_httpRequest.getRemoteAddr() : "";
    }

    public boolean isInNetwork(String p_cidrNotation) {
        final String ip = getIpAddress();
        if (Util.allNotBlank(ip, p_cidrNotation)) {
            final SubnetUtils utils = new SubnetUtils(p_cidrNotation);
            return utils.getInfo().isInRange(ip);
        }
        return false;
    }

    public IAuthorizationGrant getGrant() {
        return m_grant;
    }

    public String getAuthLevel() {
        return m_grant.getAuthLevel();
    }

    public String getAuthMode() {
        return m_grant.getAuthMode();
    }

    public String getClientClaim(String p_claimName) {
        //return ServerUtil.getAttributeValueByName(getGrant().getClient().getCustomAttributes(), p_claimName);
        return getEntryAttributeValue(getGrant().getClientDn(), p_claimName);
    }

    public String getUserClaim(String p_claimName) {
        final String ldap = ClaimMappingConfiguration.getMappingByClaim(p_claimName).getLdap();
        //return ServerUtil.getAttributeValueByName(getGrant().getUser().getCustomAttributes(), ldap);
        return getEntryAttributeValue(getGrant().getUserDn(), ldap);
    }

    public String getUserClaimByLdapName(String p_ldapName) {
//        return ServerUtil.getAttributeValueByName(getGrant().getUser().getCustomAttributes(), p_ldapName);
        return getEntryAttributeValue(getGrant().getUserDn(), p_ldapName);
    }

    public UmaRPT getRpt() {
        return m_rpt;
    }

    public ResourceSetPermission getPermission() {
        return m_permission;
    }

    private SearchResultEntry getEntryByDn(String p_dn) {
        final LDAPConnectionPool pool = ldapEntryManager.getLdapOperationService().getConnectionPool();
        try {
            return pool.getEntry(p_dn);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    private String getEntryAttributeValue(String p_dn, String p_attributeName) {
        final SearchResultEntry entry = getEntryByDn(p_dn);
        if (entry != null) {
            final Attribute attribute = entry.getAttribute(p_attributeName);
            if (attribute != null) {
                return attribute.getValue();
            }
        }
        return "";
    }
}
