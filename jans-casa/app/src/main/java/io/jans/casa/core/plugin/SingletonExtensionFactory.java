package io.jans.casa.core.plugin;

import org.pf4j.DefaultExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jgomer
 */
public class SingletonExtensionFactory extends DefaultExtensionFactory {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Object> singletons = new HashMap<>();

    @Override
    public <T> T create(Class<T> extensionClass) {

        String className = extensionClass.getName();
        Object obj = singletons.get(className);

        if (obj == null) {
            obj = super.create(extensionClass);
            if (obj != null) {
                singletons.put(className, obj);
            }
        }
        return extensionClass.cast(obj);

    }

    void removeSingleton(String extensionClassName) {
        if (singletons.remove(extensionClassName) != null) {
            logger.info("Extension for {} was removed", extensionClassName);
        }
    }

}
