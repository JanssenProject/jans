package org.gluu.service.util;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which provides external resources for injection
 *
 * @author Yuriy Movchan Date: 14/03/2017
 */
@ApplicationScoped
public class Resources {

    private static Map<String, Logger> CACHE = new HashMap<String, Logger>();

    @Produces
    public Logger getLogger(InjectionPoint ip) {
        Class<?> clazz = ip.getMember().getDeclaringClass();

        String clazzName = clazz.getName();
        Logger cached = CACHE.get(clazzName);
        if (cached != null) {
            return cached;
        }

        Logger logger = LoggerFactory.getLogger(clazz);
        CACHE.put(clazzName, logger);
        return logger;
    }
}
