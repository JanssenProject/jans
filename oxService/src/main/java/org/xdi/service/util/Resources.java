package org.xdi.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.HashMap;
import java.util.Map;

/**
 * Class which provides external resources for injection
 *
 * @author Yuriy Movchan Date: 14/03/2017
 */
public class Resources {

    private static Map<String, Logger> cache = new HashMap<String, Logger>();

	@Produces
	public Logger getLogger(InjectionPoint ip) {
		Class<?> clazz = ip.getMember().getDeclaringClass();

        String clazzName = clazz.getName();
        Logger cached = cache.get(clazzName);
        if (cached != null) {
            return cached;
        }

        Logger logger = LoggerFactory.getLogger(clazz);
        cache.put(clazzName, logger);
        return logger;
	}
}
