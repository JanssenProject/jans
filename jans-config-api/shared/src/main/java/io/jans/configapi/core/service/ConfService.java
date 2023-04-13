/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.MappingException;
import io.jans.orm.model.AttributeData;
import io.jans.orm.reflect.property.PropertyAnnotation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

import org.slf4j.Logger;

@ApplicationScoped
public class ConfService {

    private static String dn = "ou=jans-auth,ou=configuration,o=jans";

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationService configurationService;

    public Conf findConf() {
        return persistenceEntryManager.find(dn, Conf.class, null);
    }

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamic();
    }

    public <T> List<AttributeData> getEntityAttributeData(Object entry) {
        logger.info("Param to get Entity Attribute Data is entry:{}", entry);
        List<AttributeData> attributes = null;
        if (entry == null) {
            return attributes;
        }

        // Check entry class
        Class<?> entryClass = entry.getClass();
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        attributes = persistenceEntryManager.getAttributesListForPersist(entry, propertiesAnnotations);

        logger.info("Attributes for objectClass:{} are attributes:{}", entry, attributes);
        return attributes;
    }

    public <T> List<PropertyAnnotation> getEntryPropertyAnnotations(Class<T> entryClass) {
        return persistenceEntryManager.getEntryPropertyAnnotations(entryClass);
    }

}
