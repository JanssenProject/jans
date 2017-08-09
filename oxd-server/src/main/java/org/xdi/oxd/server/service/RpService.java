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

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 */

public class RpService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RpService.class);

    public static final String DEFAULT_SITE_CONFIG_JSON = "oxd-default-site-config.json";

    private static final int FILE_NAME_LENGTH = (UUID.randomUUID().toString() + ".json").length();

    private final Map<String, Rp> sites = Maps.newConcurrentMap();

    private ConfigurationService configurationService;

    private ValidationService validationService;

    private PersistenceService persistenceService;

    @Inject
    public RpService(ConfigurationService configurationService, ValidationService validationService, PersistenceService persistenceService) {
        this.configurationService = configurationService;
        this.validationService = validationService;
        this.persistenceService = persistenceService;
    }

    public void removeAllRps() {
        persistenceService.removeAllRps();
    }

    public void load() {
        for (Rp rp : persistenceService.getRps()) {
            put(rp);
        }

        final List<File> files = Lists.newArrayList(Files.fileTreeTraverser().children(configurationService.getConfDirectoryFile()));
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(DEFAULT_SITE_CONFIG_JSON)) {
                LOG.trace("Loading site file name: {}", file.getName());
                Rp rp = parseRp(file);
                if (rp != null) {
                    sites.put(DEFAULT_SITE_CONFIG_JSON, rp);
                }
            }
            if (file.getName().length() == FILE_NAME_LENGTH && file.getName().endsWith(".json")) {
                LOG.trace("Loading site file name: {}", file.getName());

                try {
                    Rp rp = parseRp(new FileInputStream(file));
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

    public Rp defaultRp() {
        Rp rp = sites.get(DEFAULT_SITE_CONFIG_JSON);
        if (rp == null) {
            LOG.error("Failed to load fallback configuration!");
            rp = new Rp();
        }
        return rp;
    }

    public Rp getRp(String id) {
        Preconditions.checkNotNull(id);
        Preconditions.checkState(!Strings.isNullOrEmpty(id));

        Rp site = sites.get(id);
        return validationService.validate(site);
    }

    public Map<String, Rp> getRps() {
        return Maps.newHashMap(sites);
    }

    public static Rp parseRp(InputStream p_stream) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(p_stream, Rp.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public static Rp parseRp(File file) {
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

    public static Rp parseRp(String rpAsJson) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(rpAsJson, Rp.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }


    public void update(Rp rp) throws IOException {
        put(rp);
        persistenceService.update(rp);
    }

    public void updateSilently(Rp rp) {
        try {
            update(rp);
        } catch (IOException e) {
            LOG.error("Failed to update site configuration: " + rp, e);
        }
    }

    public void create(Rp rp) throws IOException {
        if (StringUtils.isBlank(rp.getOxdId())) {
            rp.setOxdId(UUID.randomUUID().toString());
        }

        put(rp);
        persistenceService.create(rp);
    }

    public Rp put(Rp rp) {
        return sites.put(rp.getOxdId(), rp);
    }
}
