package io.jans.casa.core;

import io.jans.casa.core.label.PluginLabelLocator;
import io.jans.casa.core.label.SystemLabelLocator;
import io.jans.casa.misc.CssRulesResolver;
import io.jans.casa.misc.WebUtils;
import org.slf4j.Logger;
import org.zkoss.util.resource.LabelLocator;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.WebApp;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jgomer
 */
@Named("zkService")
@ApplicationScoped
public class ZKService {

    public static final String EXTERNAL_LABELS_DIR = "labels";
    private static final String WAR_LABELS_LOCATION = "/WEB-INF/classes/labels";
    private static final String FILESYSTEM_LABELS_LOCATION = System.getProperty("server.base") + File.separator + "static/i18n";

    @Inject
    private Logger logger;

    @Inject
    private ConfigurationHandler confHandler;

    private String contextPath;

    private String appName;

    private Map<String, PluginLabelLocator> labelLocators;

    private ServletContext servletContext;

    //A set of "known" locales
    private Set<Locale> supportedLocales;

    @PostConstruct
    private void inited() {
        labelLocators = new HashMap<>();
        logger.info("ZK initialized");
    }

    public void init(WebApp app) {

        try {
            servletContext = app.getServletContext();
            contextPath = servletContext.getContextPath();
            confHandler.init();
            readSystemLabels();

            appName = Optional.ofNullable(Labels.getLabel("general.appName")).orElse(app.getAppName());
            app.setAppName(appName);

            CssRulesResolver.init(servletContext);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public String getAppName() {
        return appName;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Set<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    private void readSystemLabels() {

        logger.info("Loading application labels");
        List<LabelLocator> locators = new ArrayList<>();

        List<String> propFilesPaths = Optional.ofNullable(servletContext.getResourcePaths(WAR_LABELS_LOCATION))
                .orElse(Collections.emptySet()).stream().filter(path -> path.endsWith(".properties")).collect(Collectors.toList());
        supportedLocales = new HashSet<>();

        if (propFilesPaths.size() > 0) {
            String base, temp = propFilesPaths.get(0);

            try {
                //temp is prefixed with WAR_LABELS_LOCATION
                base = servletContext.getResource(temp).toString();
                base = base.substring(0, base.length() - (temp.length() - WAR_LABELS_LOCATION.length()) + 1);
                //Here warLabelBase looks like file://.../WEB-INF/classes/labels/
                logger.trace("War labels base is {}", base);
            } catch (Exception e) {
                base = null;
                logger.error(e.getMessage(), "Unable to determine war base");
            }
            if (base != null) {
                String warLabelsBase = base;
                HashSet<String> modules = new HashSet<>();
                fillModules(propFilesPaths, WAR_LABELS_LOCATION, modules, supportedLocales);

                logger.info("War resource bundles are: {}", modules.toString());
                modules.stream().map(module -> new SystemLabelLocator(warLabelsBase, module)).forEach(locators::add);
            }
        }

        Path fsPath = Paths.get(FILESYSTEM_LABELS_LOCATION);
        if (Files.isDirectory(fsPath)) {
            String base = fsPath.toUri().toString();

            try (DirectoryStream<Path> dstream = Files.newDirectoryStream(fsPath, "*.properties")) {

                HashSet<String> modules = new HashSet<>();
                List<String> paths = new ArrayList<>();

                dstream.forEach(path -> paths.add(path.toString()));
                fillModules(paths, FILESYSTEM_LABELS_LOCATION, modules, supportedLocales);

                logger.info("External resource bundles are: {}", modules.toString());
                modules.stream().map(module -> new SystemLabelLocator(base, module)).forEach(locators::add);

            } catch (Exception e) {
                logger.error("An error occurred while processing labels at {}", FILESYSTEM_LABELS_LOCATION);
                logger.error(e.getMessage(), e);
            }
        }

        logger.debug("Locales supported are: {}", supportedLocales.toString());

        if (locators.isEmpty()) {
            logger.warn("No application labels will be available");
            logger.info("Check '{}' or '{}' contain properties files", WAR_LABELS_LOCATION, FILESYSTEM_LABELS_LOCATION);
            logger.info("Check Casa docs to learn how to support more locales and add resource bundles (localization)");
        } else {
            locators.forEach(Labels::register);
            logger.info("Labels registered");
        }

    }

    String getAppFileSystemRoot() {
        return servletContext.getRealPath("/");
    }

    void readPluginLabels(String id, Path path) {
        logger.info("Registering labels of plugin {}", id);
        PluginLabelLocator pll = new PluginLabelLocator(path, EXTERNAL_LABELS_DIR);
        labelLocators.put(id, pll);
        Labels.register(pll);
    }

    void removePluginLabels(String id) {
        try {
            PluginLabelLocator locator = labelLocators.get(id);
            if (locator != null) {
                logger.debug("Closing label locator {}", id);
                labelLocators.remove(id);
                locator.close();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    void refreshLabels() {
        logger.info("Refreshing labels");
        Labels.reset();
    }

    private void fillModules(List<String> propFilePath, String location, Set<String> modules, Set<Locale> locales) {

        //location prefixes all elements in propFilePath
        for (String path : propFilePath) {
            int idx = path.lastIndexOf(".");
            String temp = path.substring(location.length() + 1, idx);
            //temp here contains only filename (without extension)
            idx = temp.indexOf("_");

            if (idx == -1) {
                //No locale suffix
                modules.add(temp);
                //add default locale
                locales.add(WebUtils.DEFAULT_LOCALE);
            } else {
                modules.add(temp.substring(0, idx));
                //Locale is after the underscore
                if (idx + 1 < temp.length()) {
                    temp = temp.substring(idx + 1).replaceAll("_", "-");
                    try {
                        locales.add(Locale.forLanguageTag(temp));
                    } catch (Exception e) {
                        logger.warn("Unknown locale suffix '{}'", temp);
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }

    }

}
