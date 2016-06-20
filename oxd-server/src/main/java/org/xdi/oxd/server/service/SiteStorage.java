package org.xdi.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/06/2016
 */

public class SiteStorage {

    private static final Logger LOG = LoggerFactory.getLogger(SiteStorage.class);

    private final Map<String, SiteConfiguration> sites = Maps.newConcurrentMap();
    private final Map<String, File> siteFiles = Maps.newConcurrentMap();
    private final Map<String, Object> locks = Maps.newConcurrentMap();

    @Inject
    private ConfigurationService configuration;

    public Map<String, File> getSiteFiles() {
        return siteFiles;
    }

    public Map<String, SiteConfiguration> getSites() {
        return sites;
    }

    public void put(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String name = file.getName();

            sites.put(name, SiteConfigurationService.createConfiguration(fis));
            siteFiles.put(name, file);
            locks.put(name, new Object());

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    public void update(SiteConfiguration siteConfiguration) throws IOException {

        String fileName = siteConfiguration.getOxdId() + ".json";

        synchronized (getLock(fileName)) {
            File file = siteFiles.get(fileName);
            Preconditions.checkNotNull(file);
            CoreUtils.createJsonMapper().writerWithDefaultPrettyPrinter().writeValue(file, siteConfiguration);

            sites.put(file.getName(), siteConfiguration);
        }
    }

    private Object getLock(String fileName) {
        Object lock = locks.get(fileName);
        if (lock == null) {
            locks.put(fileName, new Object());
        }
        return lock;
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
        String filePath = configuration.getConfDirectoryPath();
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
