package io.jans.ca.server.service;

import io.jans.ca.common.Jackson2;
import io.jans.ca.server.configuration.model.Rp;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author yuriyz
 */
public class MigrationService {

    private MigrationService() {

    }

    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);

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
            return Jackson2.createJsonMapper().readValue(rpAsJson, Rp.class);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

}
