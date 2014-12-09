/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.service.custom.conf.CustomScript;
import org.xdi.oxauth.service.custom.conf.CustomScriptType;

import com.unboundid.ldap.sdk.Filter;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
public abstract class AbstractCustomScriptService implements Serializable {

	private static final long serialVersionUID = -6187179012715072064L;

	@Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    public void add(CustomScript customScript) {
       ldapEntryManager.persist(customScript);
    }

    public void update(CustomScript customScript) {
       ldapEntryManager.merge(customScript);
    }

    public void remove(CustomScript customScript) {
        ldapEntryManager.remove(customScript);
     }

    public CustomScript getCustomScriptByDn(String customScriptDn) {
		return ldapEntryManager.find(CustomScript.class, customScriptDn);
	}

    public List<CustomScript> findAllCustomScripts(String[] returnAttributes) {
        String baseDn = baseDn();

        List<CustomScript> result = ldapEntryManager.findEntries(baseDn, CustomScript.class, returnAttributes, null);

		return result;
	}

    public List<CustomScript> findCustomScripts(List<CustomScriptType> customScriptTypes, String[] returnAttributes) {
        String baseDn = baseDn();
        
        if ((customScriptTypes == null) || (customScriptTypes.size() == 0)) {
        	return findAllCustomScripts(returnAttributes);
        }
        
        List<Filter> customScriptTypeFilters = new ArrayList<Filter>();
        for (CustomScriptType customScriptType : customScriptTypes) {
        	Filter customScriptTypeFilter = Filter.createEqualityFilter("oxScriptType", customScriptType.getValue());
        	customScriptTypeFilters.add(customScriptTypeFilter);
        }
        		
        Filter filter = Filter.createORFilter(customScriptTypeFilters);

        List<CustomScript> result = ldapEntryManager.findEntries(baseDn, CustomScript.class, returnAttributes, filter);

		return result;
	}

    public String buildDn(String customScriptId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("inum=%s,", customScriptId));
        dn.append(baseDn());
        return dn.toString();
    }

    public abstract String baseDn();

	/**
     * Get CustomScriptService instance
     *
     * @return CustomScriptService instance
     */
    public static AbstractCustomScriptService instance() {
        if (!(Contexts.isEventContextActive() || Contexts.isApplicationContextActive())) {
            Lifecycle.beginCall();
        }

        return (AbstractCustomScriptService) Component.getInstance(AbstractCustomScriptService.class);
    }

}
