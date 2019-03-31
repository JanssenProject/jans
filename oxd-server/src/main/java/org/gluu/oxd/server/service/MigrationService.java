package org.gluu.oxd.server.service;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxd.common.CoreUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author yuriyz
 */
public class MigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);

    private static final int FILE_NAME_LENGTH = (UUID.randomUUID().toString() + ".json").length();

    private ConfigurationService configurationService;

    private RpService rpService;

    @Inject
    public MigrationService(ConfigurationService configurationService, RpService rpService) {
        this.configurationService = configurationService;
        this.rpService = rpService;
    }

    public void migrate() {
        File migrationFolderFile = getMigrationFolderFile();
        if (migrationFolderFile == null) {
            LOG.debug("Skip migration because migration source folder is not specified or otherwise invalid.");
            return;
        }

        migrateChildren(migrationFolderFile);
    }

    public void migrateChildren(File parentFolder) {
        final List<File> files = Lists.newArrayList(Files.fileTreeTraverser().children(parentFolder));
        for (File file : files) {
            migrateRpFile(file);
        }
    }

    private void migrateRpFile(File file) {
        if (file.getName().length() == FILE_NAME_LENGTH && file.getName().endsWith(".json")) {
            LOG.trace("Loading rp file name: {}", file.getName());

            try {
                Rp rp = parseRp(file);
                rpService.create(rp);

//                String path = file.getAbsolutePath();
//                if (file.delete()) {
//                    LOG.debug("Removed rp file : " + path + " and pushed it to database.");
//                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public static Rp parseRp(File file) {
        try {
            return parseRp(FileUtils.readFileToString(file));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public static Rp parseRp(String rpAsJson) {
        try {
            if (StringUtils.isBlank(rpAsJson)) {
                return null;
            }
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

    private File getMigrationFolderFile() {
        String migrationSourceFolderPath = configurationService.getConfiguration().getMigrationSourceFolderPath();
        if (StringUtils.isBlank(migrationSourceFolderPath)) {
            LOG.debug("Migration source folder is not specified.");
            return null;
        }

        File migrationFolder = new File(migrationSourceFolderPath);
        if (!migrationFolder.exists() || !migrationFolder.isDirectory()) {
            LOG.error("Migration source folder does not exist or is not directory.");
            return null;
        }
        return migrationFolder;
    }
}
