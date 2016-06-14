package org.xdi.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/09/2015
 */

public class SiteConfigurationService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SiteConfigurationService.class);

    private static final String DEFAULT_SITE_CONFIG_JSON = "oxd-default-site-config.json";

    private static final int FILE_NAME_LENGTH = (UUID.randomUUID().toString() + ".json").length();

    private final Map<String, SiteConfiguration> sites = Maps.newConcurrentMap();
    private final Map<String, File> siteFiles = Maps.newConcurrentMap();

    @Inject
    ConfigurationService configurationService;
    @Inject
    ValidationService validationService;

    public void removeAllExistingConfigurations() {
        for (File file : siteFiles.values()) {
            String path = file.getAbsolutePath();
            if (file.delete()) {
                LOG.debug("Removed site configuration file : " + path);
            }
        }
    }

    public void load() {

        // load all files
        final List<File> files = Lists.newArrayList(Files.fileTreeTraverser().children(configurationService.getConfDirectoryFile()));
        for (File file : files) {
            if (!file.getName().equalsIgnoreCase(DEFAULT_SITE_CONFIG_JSON) &&
                    (file.getName().length() != FILE_NAME_LENGTH ||
                            !file.getName().endsWith(".json"))) { // precondition

                continue;
            }
            loadFile(file);
        }
    }

    private void loadFile(File file) {
        LOG.trace("Loading site file name: {}", file.getName());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            sites.put(file.getName(), createConfiguration(fis));
            siteFiles.put(file.getName(), file);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    public SiteConfiguration defaultSiteConfiguration() {
        SiteConfiguration siteConfiguration = sites.get(DEFAULT_SITE_CONFIG_JSON);
        if (siteConfiguration == null) {
            LOG.error("Failed to load fallback configuration!");
            siteConfiguration = new SiteConfiguration();
        }
        return siteConfiguration;
    }

    public SiteConfiguration getSite(String id) {
        Preconditions.checkNotNull(id);
        Preconditions.checkState(!Strings.isNullOrEmpty(id));

        SiteConfiguration site = sites.get(id + ".json");
        return validationService.validate(site);
    }

    public Map<String, SiteConfiguration> getSites() {
        return Maps.newHashMap(sites);
    }

    public static SiteConfiguration createConfiguration(InputStream p_stream) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(p_stream, SiteConfiguration.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public void update(SiteConfiguration siteConfiguration) throws IOException {
        String fileName = siteConfiguration.getOxdId() + ".json";

        File file = siteFiles.get(fileName);
        Preconditions.checkNotNull(file);
        CoreUtils.createJsonMapper().writerWithDefaultPrettyPrinter().writeValue(file, siteConfiguration);

        sites.put(file.getName(), siteConfiguration);
    }

    public void updateSilently(SiteConfiguration siteConfiguration) {
        try {
            update(siteConfiguration);
        } catch (IOException e) {
            LOG.error("Failed to update site configuration: " + siteConfiguration, e);
        }
    }

    public void createNewFile(SiteConfiguration siteConfiguration) throws IOException {
        String fileName = siteConfiguration.getOxdId() + ".json";

        File file = siteFiles.get(fileName);
        if (file == null) {
            file = createSiteFile(fileName);
        }
        CoreUtils.createJsonMapper().writerWithDefaultPrettyPrinter().writeValue(file, siteConfiguration);

        sites.put(file.getName(), siteConfiguration);
        siteFiles.put(file.getName(), file);
    }

    private File createSiteFile(String fileName) throws IOException {
        String filePath = configurationService.getConfDirectoryPath();
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        filePath = filePath + fileName;

        final File newFile = new File(filePath);
        if (newFile.createNewFile()) {
            siteFiles.put(fileName, newFile);
        }
        return newFile;
    }
}
