package org.xdi.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public static final String DEFAULT_SITE_CONFIG_JSON = "oxd-default-site-config.json";

    private static final int FILE_NAME_LENGTH = (UUID.randomUUID().toString() + ".json").length();

    private final Map<String, SiteConfiguration> sites = Maps.newConcurrentMap();

    private ConfigurationService configurationService;

    private ValidationService validationService;

    private PersistenceService persistenceService;

    @Inject
    public SiteConfigurationService(ConfigurationService configurationService, ValidationService validationService, PersistenceService persistenceService) {
        this.configurationService = configurationService;
        this.validationService = validationService;
        this.persistenceService = persistenceService;
    }

    public void removeAllRps() {
        persistenceService.removeAllRps();
    }

    public void load() {
        for (SiteConfiguration rp : persistenceService.getRps()) {
            put(rp);
        }

        final List<File> files = Lists.newArrayList(Files.fileTreeTraverser().children(configurationService.getConfDirectoryFile()));
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(DEFAULT_SITE_CONFIG_JSON)) {
                LOG.trace("Loading site file name: {}", file.getName());
                SiteConfiguration rp = parseRp(file);
                if (rp != null) {
                    sites.put(DEFAULT_SITE_CONFIG_JSON, rp);
                }
            }
            if (file.getName().length() == FILE_NAME_LENGTH && file.getName().endsWith(".json")) {
                LOG.trace("Loading site file name: {}", file.getName());

                try {
                    SiteConfiguration rp = parseRp(new FileInputStream(file));
                    create(rp);

                    String path = file.getAbsolutePath();
                    if (file.delete()) {
                        LOG.debug("Removed site configuration file : " + path + " and pushed it to database.");
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
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

        SiteConfiguration site = sites.get(id);
        return validationService.validate(site);
    }

    public Map<String, SiteConfiguration> getSites() {
        return Maps.newHashMap(sites);
    }

    public static SiteConfiguration parseRp(InputStream p_stream) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(p_stream, SiteConfiguration.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public static SiteConfiguration parseRp(File file) {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return parseRp(fis);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return null;
    }

    public static SiteConfiguration parseRp(String rpAsJson) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(rpAsJson, SiteConfiguration.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }


    public void update(SiteConfiguration rp) throws IOException {
        put(rp);
        persistenceService.update(rp);
    }

    public void updateSilently(SiteConfiguration siteConfiguration) {
        try {
            update(siteConfiguration);
        } catch (IOException e) {
            LOG.error("Failed to update site configuration: " + siteConfiguration, e);
        }
    }

    public void create(SiteConfiguration siteConfiguration) throws IOException {
        if (StringUtils.isBlank(siteConfiguration.getOxdId())) {
            siteConfiguration.setOxdId(UUID.randomUUID().toString());
        }

        put(siteConfiguration);
        persistenceService.create(siteConfiguration);
    }

    public SiteConfiguration put(SiteConfiguration rp) {
        return sites.put(rp.getOxdId(), rp);
    }
}
