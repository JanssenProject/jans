/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service.custom.script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;

import com.google.common.base.Optional;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
public abstract class AbstractCustomScriptService implements Serializable {

    private static final long serialVersionUID = -6187179012715072064L;

    @Inject
    protected Logger log;

    @Inject
    protected PersistenceEntryManager persistenceEntryManager;

    public void add(CustomScript customScript) {
        persistenceEntryManager.persist(customScript);
    }

    public void update(CustomScript customScript) {
        persistenceEntryManager.merge(customScript);
    }

    public void remove(CustomScript customScript) {
        persistenceEntryManager.remove(customScript);
    }

    public CustomScript getCustomScriptByDn(String customScriptDn, String... returnAttributes) {
        return persistenceEntryManager.find(customScriptDn, CustomScript.class, returnAttributes);
    }

    public CustomScript getCustomScriptByDn(Class<?> customScriptType, String customScriptDn) {
        return (CustomScript) persistenceEntryManager.find(customScriptType, customScriptDn);
    }

    public Optional<CustomScript> getCustomScriptByINum(String baseDn, String inum, String... returnAttributes) {

        final List<Filter> customScriptTypeFilters = new ArrayList<Filter>();

        final Filter customScriptTypeFilter = Filter.createEqualityFilter("inum", inum);
        customScriptTypeFilters.add(customScriptTypeFilter);

        final Filter filter = Filter.createORFilter(customScriptTypeFilters);

        final List<CustomScript> result = persistenceEntryManager.findEntries(baseDn, CustomScript.class, filter, returnAttributes);

        if (result.isEmpty()) {

            return  Optional.absent();
        }

        return Optional.of(result.get(0));
    }


    public List<CustomScript> findAllCustomScripts(String[] returnAttributes) {
        String baseDn = baseDn();

        List<CustomScript> result = persistenceEntryManager.findEntries(baseDn, CustomScript.class, null, returnAttributes);

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

        List<CustomScript> result = persistenceEntryManager.findEntries(baseDn, CustomScript.class, filter, returnAttributes);

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
