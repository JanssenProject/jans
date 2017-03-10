/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import java.util.Arrays;
import java.util.List;


import org.jboss.seam.log.Logging;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;

/**
 * LDAP Utilities.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/07/2012
 */

public class LdapUtils {

    private final static Log LOG = Logging.getLog(LdapUtils.class);

    /**
     * Avoid initialization
     */
    private LdapUtils() {
    }

    public static boolean isDN(String p_dn) {
        return DN.isValidDN(p_dn);
    }

    public static boolean isValidDNs(String... p_dnList) {
        return isValidDNs(p_dnList != null ? Arrays.asList(p_dnList) : null);
    }

    public static boolean isValidDNs(List<String> p_dnList) {
        if (p_dnList != null) {
            for (String dn : p_dnList) {
                if (!isDN(dn)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates any filter to load all objects represented by this dn list.
     *
     * @param p_filterAttributeName filter attribute name
     * @param p_dnList              dn list
     * @return filter
     */
    public static Filter createAnyFilterFromDnList(String p_filterAttributeName, List<String> p_dnList) {
        try {
            if (p_dnList != null && !p_dnList.isEmpty()) {
                final StringBuilder sb = new StringBuilder("(|");
                for (String dn : p_dnList) {
                    final DN dnObj = new DN(dn);
                    final RDN rdn = dnObj.getRDN();
                    if (rdn.getAttributeNames()[0].equals(p_filterAttributeName)) {
                        final String[] values = rdn.getAttributeValues();
                        if (values != null && values.length == 1) {
                            sb.append("(");
                            sb.append(p_filterAttributeName).append("=");
                            sb.append(values[0]);
                            sb.append(")");
                        }
                    }
                }
                sb.append(")");
                final String filterAsString = sb.toString();
                log.trace("dnList: " + p_dnList + ", ldapFilter: " + filterAsString);
                return Filter.create(filterAsString);
            }
        } catch (LDAPException e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }
}
