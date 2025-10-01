package io.jans.casa.misc;

import io.jans.casa.core.ExtensionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.lang.ContextClassLoaderFactory;

import java.util.Arrays;

/**
 * A class implementing interface <code>org.zkoss.lang.ContextClassLoaderFactory</code>. Basically it allows to know
 * based on (string) name, which classloader a class belongs to.
 * <p>This is useful for the ZK rendering engine to instantiate view model classes.</p>
 * @author jgomer
 */
public class CustomClassLoader implements ContextClassLoaderFactory {

    private static final String[] DEFAULT_PACKAGES = {"org.zkoss", "java", "jakarta", "javax"};

    private Logger logger = LoggerFactory.getLogger(getClass());
    private ExtensionsManager extManager;

    /**
     * Constructs an instance of this object.
     */
    public CustomClassLoader() {
        extManager = Utils.managedBean(ExtensionsManager.class);
        if (extManager == null) {
            logger.error("Could not obtain a reference to ExtensionsManager bean");
        }
    }

    /**
     * Returns the context ClassLoader for the reference class.
     * @param reference the reference class where it is invoked from.
     */
    //what senses does this method have?
    public ClassLoader getContextClassLoader(Class<?> reference) {
        return reference.getClassLoader();
        //return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Returns the context ClassLoader for a class name.
     * @param className the reference class name where it is invoked from.
     * @return A java.lang.ClassLoader
     */
    public ClassLoader getContextClassLoaderForName(String className) {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        //Filter out uninteresting classes
        if (Arrays.stream(DEFAULT_PACKAGES).anyMatch(pkg -> className.startsWith(pkg + "."))
                || !Character.isLetter(className.charAt(0)) || className.equals("event")) {
            return loader;
        }

        try {
            loader.loadClass(className);
            //The following ends up being to noisy
            //logger.trace("Class '{}' found in current thread's context class loader", className);
            return loader;
        } catch (ClassNotFoundException e) {

            //logger.warn("Class not found in current thread's context class loader");
            if (extManager != null) {
                loader = extManager.getPluginClassLoader(className);

                if (loader == null) {
                    logger.error("Could not find a plugin class loader for class '{}'", className);
                } else {
                    logger.trace("Class '{}' found in one of the plugins class loaders", className);
                }
            }
        }
        return loader;

    }

}
