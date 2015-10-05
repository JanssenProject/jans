package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.xdi.oxd.Tester;
import org.xdi.oxd.web.TestAppModule;

import java.util.UUID;

import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */
@Guice(modules = TestAppModule.class)
public class SiteConfigurationServiceTest {

    @Inject
    SiteConfigurationService service;

    @BeforeClass
    public void setUp() {
        Tester.setSystemConfPath();
        service.load();
    }

    @Test
    public void load() throws Exception {
        assertTrue(service.getSites().size() == 2);
    }

    @Test
    public void persist() throws Exception {
        service.persist(newSiteConfiguration());
        assertTrue(service.getSites().size() == 3);
    }

    public SiteConfiguration newSiteConfiguration() {
        SiteConfiguration conf = new SiteConfiguration(service.defaultSiteConfiguration());
        conf.setOxdId(UUID.randomUUID().toString());
        return conf;
    }

}
