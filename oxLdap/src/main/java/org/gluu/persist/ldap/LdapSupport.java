package org.gluu.persist.ldap;

import java.util.List;

import org.gluu.search.filter.Filter;

/**
 * Specialized LDAp operations which Entry Manager can provide
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface LdapSupport {

    String[] getLDIF(String dn);

    List<String[]> getLDIF(String dn, String[] attributes);

    List<String[]> getLDIFTree(String baseDN, Filter searchFilter, String... attributes);

}
