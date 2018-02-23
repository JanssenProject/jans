package org.xdi.oxd;

import org.xdi.oxd.server.service.ConfigurationService;

import java.io.File;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class Tester {

    private Tester() {
    }

    public static void setSystemConfPath() {
        System.setProperty(ConfigurationService.CONF_SYS_PROPERTY_NAME, getConfPath());
        System.out.println(ConfigurationService.CONF_SYS_PROPERTY_NAME + " = " + System.getProperty(ConfigurationService.CONF_SYS_PROPERTY_NAME));
    }

    public static String getConfPath() {
        String workingDir = System.getProperty("user.dir");
        System.out.println("Working Directory = " + workingDir);
        return workingDir + "/oxd-server/src/test/resources/" + File.separator + ConfigurationService.TEST_FILE_NAME;
    }
}
