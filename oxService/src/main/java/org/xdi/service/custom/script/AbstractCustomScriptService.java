/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.custom.script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.model.CustomScript;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
public abstract class AbstractCustomScriptService implements Serializable {

    private static final long serialVersionUID = -6187179012715072064L;

    @Inject
    private Logger log;

    @Inject
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

    public CustomScript getCustomScriptByDn(String customScriptDn, String... returnAttributes) {
        return ldapEntryManager.find(CustomScript.class, customScriptDn, returnAttributes);
    }

    public CustomScript getCustomScriptByDn(Class<?> customScriptType, String customScriptDn) {
        return (CustomScript) ldapEntryManager.find(customScriptType, customScriptDn);
    }

    public List<CustomScript> findAllCustomScripts(String[] returnAttributes) {
        String baseDn = baseDn();

        List<CustomScript> result = ldapEntryManager.findEntries(baseDn, CustomScript.class, null, returnAttributes);

        return result;
    }

    public List<CustomScript> findCustomScripts(List<CustomScriptType> customScriptTypes, String... returnAttributes) {
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

        List<CustomScript> result = ldapEntryManager.findEntries(baseDn, CustomScript.class, filter, returnAttributes);

        return result;
    }

    public String buildDn(String customScriptId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("inum=%s,", customScriptId));
        dn.append(baseDn());
        return dn.toString();
    }

    public abstract String baseDn();

}
