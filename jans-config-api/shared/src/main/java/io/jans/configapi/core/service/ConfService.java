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
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.util.ReflectHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

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

    public <T> List<PropertyAnnotation> getEntryPropertyAnnotations(Class<T> entryClass) {
        return persistenceEntryManager.getEntryPropertyAnnotations(entryClass);
    }

    public Map<String, List<Annotation>> getPropertiesAnnotations(Class<?> theClass, Class<?>... allowedAnnotations) {

        logger.info("Getting Properties Annotations for theClass:{}, allowedAnnotations:{}", theClass,
                allowedAnnotations);
        Map<String, List<Annotation>> propertiesAnnotations = null;
        if (theClass == null || allowedAnnotations == null || allowedAnnotations.length == 0) {
            return propertiesAnnotations;
        }

        propertiesAnnotations = ReflectHelper.getPropertiesAnnotations(theClass, allowedAnnotations);

        logger.info("Properties Annotations for theClass:{}, allowedAnnotations:{} are propertiesAnnotations:{}",
                theClass, allowedAnnotations, propertiesAnnotations);

        return propertiesAnnotations;
    }

}
