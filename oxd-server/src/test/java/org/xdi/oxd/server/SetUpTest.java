/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.service.SiteConfigurationService;

/**
 * Main class to set up and tear down suite.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class SetUpTest {

    @BeforeSuite
    public static void beforeSuite() {
        CoreUtils.createExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ServerLauncher.start();
                ServerLauncher.getInjector().getInstance(SiteConfigurationService.class).removeAllExistingConfigurations();
            }
        });
        // from one side we should give time to start server, from other we can't start in current
        // thread because it will block suite thread, ugly but works...
        CoreUtils.sleep(7);
    }

    @AfterSuite
    public static void afterSuite() {
        ServerLauncher.shutdown();
    }

}
