/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gluu.oxeleven.model.Configuration;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;

import java.io.File;

/**
 * @author Javier Rojas Blum
 * @version May 20, 2016
 */
@Scope(ScopeType.APPLICATION)
@Name("configurationService")
@AutoCreate
@Startup
public class ConfigurationService {

    private static final Log LOG = Logging.getLog(ConfigurationService.class);

    static {
        BASE_DIR = System.getProperty("catalina.home");
    }

    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;
    private static final String CONFIG_FILE_PATH = DIR + "oxeleven-config.json";

    private Configuration configuration;

    @Create
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            configuration = mapper.readValue(new File(CONFIG_FILE_PATH), Configuration.class);
        } catch (Exception e) {
            LOG.error("Failed to load configuration from LDAP. Please fix it!!!.", e);
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public static ConfigurationService instance() {
        boolean createContexts = !Contexts.isEventContextActive() && !Contexts.isApplicationContextActive();
        if (createContexts) {
            Lifecycle.beginCall();
        }

        return (ConfigurationService) Component.getInstance(ConfigurationService.class);
    }
}
