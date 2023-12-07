package io.jans.casa.core;

import io.jans.casa.rest.RSInitializer;
import io.jans.casa.rest.RSResourceScope;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author jgomer
 */
@ApplicationScoped
public class RSRegistryHandler {

    private static final String ENDPOINTS_PREFIX = ExtensionsManager.PLUGINS_EXTRACTION_DIR;

    @Inject
    private Logger logger;

    @Inject
    private ServletContext servletContext;

    private Registry rsRegistry;

    private boolean enabled;

    private List<String> skipFolders;

    private Map<String, List<Class<?>>> registeredResources;

    @SuppressWarnings("unchecked")
    @PostConstruct
    private void inited() {
        try {
            skipFolders = Arrays.asList(ZKService.EXTERNAL_LABELS_DIR, ExtensionsManager.ASSETS_DIR, "META-INF");
            registeredResources = new HashMap<>();

            //Try to find a ResteasyDeployment
            Object obj = servletContext.getAttribute(Registry.class.getName());
            //sc.getAttribute(ResteasyDeployment.class.getName())

            if (obj == null) {
                obj = servletContext.getAttribute(ResteasyContextParameters.RESTEASY_DEPLOYMENTS);
                Map<String, ResteasyDeployment> deployments = (Map<String, ResteasyDeployment>) obj;
                rsRegistry = deployments.get(RSInitializer.ROOT_PATH).getRegistry();
            } else {
                rsRegistry = ((ResteasyDeployment) obj).getRegistry();
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (rsRegistry == null) {
                logger.warn("Could not access RestEasy registry. Addition of REST services at runtime may not be available");
            } else {
                logger.info("RestEasy registry is accessible. Addition of REST services at runtime will be available");
                enabled = true;
            }
        }

    }

    private boolean processClassEntry(String id, String binaryName, ClassLoader clsLoader, List<Class<?>> list) {

        boolean added = false;
        try {
            Class<?> cls = clsLoader.loadClass(binaryName);
            jakarta.ws.rs.Path pathAnnotation = cls.getAnnotation(jakarta.ws.rs.Path.class);

            if (pathAnnotation != null) {
                logger.info("Found class '{}' annotated with @Path", binaryName);
                String basePath = ENDPOINTS_PREFIX + "/" + id;
                String absolutePath = RSInitializer.ROOT_PATH + "/" + basePath + pathAnnotation.value();

                RSResourceScope scopeAnnotation = cls.getAnnotation(RSResourceScope.class);
                boolean isSingleton =  scopeAnnotation == null || scopeAnnotation.singleton();

                if (isSingleton) {
                    try {
                        rsRegistry.addSingletonResource(cls.newInstance(), basePath);
                        logger.info("Singleton resource has been bound to '{}' endpoint", absolutePath);
                        list.add(cls);
                        added = true;
                    } catch (Exception e) {
                        logger.error("Class could not be instantiated. Check it has no args constructor");
                    }
                } else {
                    rsRegistry.addPerRequestResource(cls, basePath);
                    logger.info("Per-request resource has been bound to '{}' endpoint", absolutePath);
                    list.add(cls);
                    added = true;
                }
            }

        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            //Intentionally left empty
            //In plugin's jar there can be references to classes not included in the plugin (ie unnecessary deps)
        }
        return added;

    }

    private int scanJarForRSResources(String id, JarInputStream inStream, ClassLoader clsLoader) throws IOException {

        int count = 0;
        JarEntry entry = inStream.getNextJarEntry();

        for (; entry != null; entry = inStream.getNextJarEntry()) {
            final String entryName = entry.getName();

            if (!entry.isDirectory() && entryName.endsWith(".class")
                    && skipFolders.stream().noneMatch(skip -> entryName.startsWith(skip + "/"))) {

                String binaryName = entryName.replace("/", ".");
                binaryName = binaryName.substring(0, binaryName.length() - ".class".length());
                count += processClassEntry(id, binaryName, clsLoader, registeredResources.get(id)) ? 1 : 0;
            }
        }
        return count;

    }

    public void scan(String id, Path path, ClassLoader classLoader) {

        int scannedResources = 0;
        if (enabled) {

            registeredResources.put(id, new ArrayList<>());
            //Recursively scan for @Path annotation jar file
            try (JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(path.toString())), false)) {
                scannedResources = scanJarForRSResources(id, jis, classLoader);
            } catch (IOException e) {
                logger.error("Error scanning RestEasy resources: {}", e.getMessage());
            }
            logger.info("{} RestEasy resource class(es) registered", scannedResources);

        } else {
            logger.info("Path '{}' will not be scanned: no RestEasy registry was found", path.toString());
        }

    }

    public void remove(String id) {

        if (enabled) {
            List<Class<?>> classes = registeredResources.get(id);
            if (classes != null) {
                for (Class<?> cls : classes) {
                    logger.debug("Removing RestEasy registration {}", cls.getName());
                    rsRegistry.removeRegistrations(cls, ENDPOINTS_PREFIX + "/" + id);
                }
                registeredResources.remove(id);
            }
        }

    }

}
