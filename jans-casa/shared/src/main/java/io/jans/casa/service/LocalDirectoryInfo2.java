package io.jans.casa.service;

import io.jans.casa.core.model.JansOrganization;

import java.util.Set;
import java.util.Map;

import org.json.JSONObject;
/**
 * Provides access to extra information in the local database. See {@link LocalDirectoryInfo} interface.
 */
public interface LocalDirectoryInfo2 extends LocalDirectoryInfo {

    JSONObject getAgamaFlowConfigProperties(String qname);
    
    /**
     * Returns a map with name/value pairs of the configuration properties belonging to a Gluu Server interception script
     * identified by an <code>acr</code> value.
     * @param acr ACR (display Name) value that identifies the custom script
     * @return A map. Null if no script is found associated with the acr passed
     */
    Map<String, String> getCustScriptConfigProperties(String acr);

    /**
     * Returns an instance of {@link JansOrganization} that represents the organization entry of your local Gluu Server.
     * This is the <i>o</i> entry that contains most of Gluu Server directory branches like <i>people, groups, clients, etc.</i>.
     * @return A {@link JansOrganization} object
     */
    JansOrganization getOrganization();

    /**
     * Returns a list of Object Classes as configured in underlying AS
     * @return Non-empty List of strings
     */
    Set<String> getPersonOCs();

}
