package io.jans.casa.service;

/**
 * Provides location information about important entities of Jans Server
 */
public interface LocalDirectoryInfo {

    /**
     * Returns the DN of a person in your local Gluu Server LDAP.
     * @param id ID of person (<code>inum</code> attribute value). No checks are made with regard to the value passed actually
     *           representing an existing LDAP entry
     * @return A string value
     */
    String getPersonDn(String id);

    /**
     * Returns the DN of the <i>people</i> branch in your local Gluu Server LDAP.
     * @return A string value
     */
    String getPeopleDn();

    /**
     * Returns the DN of the <i>groups</i> branch in your local Gluu Server LDAP.
     * @return A string value
     */
    String getGroupsDn();

    /**
     * Returns the DN of the <i>scopes</i> branch in your local Gluu Server LDAP.
     * @return A string value
     */
    String getScopesDn();

    /**
     * Returns the DN of the <i>clients</i> branch in your local Gluu Server LDAP.
     * @return A string value
     */
    String getClientsDn();

    /**
     * Returns the DN of the <i>scripts</i> branch in your local Gluu Server LDAP.
     * @return A string value
     */
    String getCustomScriptsDn();

    /**
     * Returns the URL of (this) authorization server. Typically, it has the form https://host
     * @return A string value
     */
    String getIssuerUrl();

}
