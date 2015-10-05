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
    }

    public static String getConfPath() {
        return Tester.class.getProtectionDomain().getCodeSource().getLocation().getPath() + File.separator + ConfigurationService.TEST_FILE_NAME;
    }
}
