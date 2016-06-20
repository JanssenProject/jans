package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.xdi.oxd.Tester;
import org.xdi.oxd.web.TestAppModule;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */
@Guice(modules = TestAppModule.class)
public class SiteConfigurationServiceTest {

    private static ExecutorService EXECUTOR_SERVICE;

    @Inject
    SiteConfigurationService service;

    @BeforeClass
    public void setUp() {
        Tester.setSystemConfPath();
        service.load();
    }

    @BeforeSuite
    public void setUpSuite() {
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(200);
    }

    @AfterSuite
    public void tearDownSuite() {
        EXECUTOR_SERVICE.shutdown();
    }

    @Test
    public void load() throws Exception {
        assertTrue(service.getSites().size() == 2);
    }

    @Test
    public void persist() throws Exception {
        service.createNewFile(newSiteConfiguration());
        assertTrue(service.getSites().size() == 3);
    }

    @Test(invocationCount = 10, threadPoolSize = 10)
    public void stressTest() throws IOException {

        final SiteConfiguration siteConfiguration = service.defaultSiteConfiguration();
        siteConfiguration.setOxdId(UUID.randomUUID().toString());
        siteConfiguration.setPat(UUID.randomUUID().toString());

        service.createNewFile(siteConfiguration);

        for (int i = 0; i < 11; i++) {
            EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        siteConfiguration.setPat(UUID.randomUUID().toString());
                        service.update(siteConfiguration);
                        System.out.println("Updated PAT: " + siteConfiguration.getPat() + ", for site: " + siteConfiguration.getOxdId());
                    } catch (Throwable e) {
                        throw new AssertionError("Failed to update configuration: " + siteConfiguration.getOxdId());
                    }
                }
            });
        }
    }

    public SiteConfiguration newSiteConfiguration() {
        SiteConfiguration conf = new SiteConfiguration(service.defaultSiteConfiguration());
        conf.setOxdId(UUID.randomUUID().toString());
        return conf;
    }

}
