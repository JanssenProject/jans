/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.keycloak.link.service;

import io.jans.link.constants.JansConstants;
import io.jans.link.model.GluuCustomPerson;
import io.jans.link.model.GluuSimplePerson;
import io.jans.keycloak.link.model.JansInumMap;
import io.jans.link.service.InumService;
import io.jans.model.JansCustomAttribute;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapFilterConverter;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.ArrayHelper;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;

/**
 * Provides Link related operations
 *
 * @author Yuriy Movchan Date: 07.04.2011
 */
@ApplicationScoped
public class LinkInterceptionService{

    public LinkInterceptionService() {
	}

	@Inject
    private Logger log;

    @Inject
    private LdapFilterConverter ldapFilterConverter;

    @Inject
    private InumService inumService;

    public Filter createFilter(String customLdapFilter) throws SearchException {
        if (StringHelper.isEmpty(customLdapFilter)) {
            return null;
        }

        return ldapFilterConverter.convertRawLdapFilterToFilter(customLdapFilter);
    }

    public Filter createFilter(String[] keyAttributes, String[] keyObjectClasses, String keyAttributeStart, Filter customFilter) throws SearchException {
        if ((keyAttributes == null) || (keyObjectClasses == null)) {
            return null;
        }

        List<Filter> filters = new ArrayList<Filter>();
        for (int i = 0; i < keyAttributes.length; i++) {
            String filterString = keyAttributes[i];

            if (filterString.contains("=")) {
                filters.add(ldapFilterConverter.convertRawLdapFilterToFilter(filterString));
                // } else {
                // filters.add(Filter.createPresenceFilter(filterString));
            }

            // Limit result list
            if ((i == 0) && (keyAttributeStart != null)) {
                int index = filterString.indexOf('=');
                if (index != -1) {
                    filterString = filterString.substring(0, index);
                }

                filterString = String.format("%s=%s*", filterString, keyAttributeStart);
                filters.add(ldapFilterConverter.convertRawLdapFilterToFilter(filterString));
            }
        }

        for (String keyObjectClass : keyObjectClasses) {
            filters.add(Filter.createEqualityFilter(OxConstants.OBJECT_CLASS, keyObjectClass));
        }

        if (customFilter != null) {
            filters.add(customFilter);
        }

        return Filter.createANDFilter(filters);
    }

    public Filter createObjectClassPresenceFilter() {
        return Filter.createPresenceFilter(OxConstants.OBJECT_CLASS);
    }

    public void addInumMap(PersistenceEntryManager ldapEntryManager, JansInumMap inumMap) {
        ldapEntryManager.persist(inumMap);
    }

    public boolean containsInumMap(PersistenceEntryManager ldapEntryManager, String dn) {
        return ldapEntryManager.contains(dn, JansInumMap.class);
    }

    public String generateInumForNewInumMap(String inumbBaseDn, PersistenceEntryManager ldapEntryManager) {
        String newInum;
        String newDn;
        do {
            newInum = generateInumForNewInumMapImpl();
            newDn = getDnForInum(inumbBaseDn, newInum);
        } while (containsInumMap(ldapEntryManager, newDn));

        return newInum;
    }

    public String getDnForInum(String baseDn, String inum) {
        return String.format("inum=%s,%s", inum, baseDn);
    }

    private String generateInumForNewInumMapImpl() {
        String inum = inumService.generateInums(JansConstants.INUM_TYPE_PEOPLE_SLUG);
        return inum;
    }

    public void setTargetEntryAttributes(GluuSimplePerson sourcePerson, Map<String, String> targetServerAttributesMapping,
                                         GluuCustomPerson targetPerson) {
        //try {
            // Collect all attributes to single map
        Map<String, JansCustomAttribute> customAttributesMap = new HashMap<String, JansCustomAttribute>();
        for (JansCustomAttribute sourceCustomAttribute : sourcePerson.getCustomAttributes()) {
            customAttributesMap.put(StringHelper.toLowerCase(sourceCustomAttribute.getName()), sourceCustomAttribute);
        }

            List<JansCustomAttribute> resultAttributes = new ArrayList<JansCustomAttribute>();

            // Add attributes configured via mapping
            Set<String> processedAttributeNames = new HashSet<>();




            for (Entry<String, String> targetServerAttributeEntry : targetServerAttributesMapping.entrySet()) {
                String sourceKeyAttributeName = StringHelper.toLowerCase(targetServerAttributeEntry.getValue());
                String targetKeyAttributeName = targetServerAttributeEntry.getKey();

                processedAttributeNames.add(sourceKeyAttributeName);



                JansCustomAttribute gluuCustomAttribute = customAttributesMap.get(sourceKeyAttributeName);
                if (gluuCustomAttribute != null) {
                String[] values = gluuCustomAttribute.getStringValues();
                String[] clonedValue = ArrayHelper.arrayClone(values);

                //String[] clonedValue = new String[0];

                //clonedValue = (String[]) PropertyUtils.getProperty(sourcePerson, targetKeyAttributeName);


                JansCustomAttribute gluuCustomAttributeCopy = new JansCustomAttribute(targetKeyAttributeName, clonedValue);
                gluuCustomAttributeCopy.setName(targetKeyAttributeName);
                resultAttributes.add(gluuCustomAttributeCopy);
                }
            }

            // Set destination entry attributes
        for (Entry<String, JansCustomAttribute> sourceCustomAttributeEntry : customAttributesMap.entrySet()) {
            if (!processedAttributeNames.contains(sourceCustomAttributeEntry.getKey())) {
                targetPerson.setAttribute(sourceCustomAttributeEntry.getValue());
            }
        }

            for (JansCustomAttribute resultAttribute : resultAttributes) {
                targetPerson.setAttribute(resultAttribute);
            }
        /*} catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }*/

    }

}
