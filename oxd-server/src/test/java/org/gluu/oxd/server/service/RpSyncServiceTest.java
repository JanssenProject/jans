package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.testing.ResourceHelpers;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.guice.GuiceModule;
import org.gluu.oxd.server.persistence.PersistenceService;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

@Guice(modules = GuiceModule.class)
public class RpSyncServiceTest {

    @Inject
    ConfigurationService configurationService;
    @Inject
    RpService service;
    @Inject
    RpSyncService rpSyncService;
    @Inject
    PersistenceService persistenceService;

    @BeforeClass
    public void setUp() throws IOException, ConfigurationException {
        configurationService.setConfiguration(parseConfiguration(ResourceHelpers.resourceFilePath("oxd-server-jenkins.yml")));
        persistenceService.create();
        service.removeAllRps();
        service.load();
    }

    @AfterSuite
    public void tearDownSuite() {
        persistenceService.destroy();
    }

    @Test
    public void testShouldSyncForNullRp() {
        Assert.assertFalse(rpSyncService.shouldSync(null));
        service.removeAllRps();
    }

    @Test
    public void testShouldSyncForNotNullRp() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        Assert.assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void testShouldSyncForSyncAllowed() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        Assert.assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void testShouldSyncForSyncNotAllowed() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(false);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        Assert.assertFalse(rpSyncService.shouldSync(rp));
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testShouldSyncForNullLastSynced() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        rp.setLastSynced(null);
        rp.setSyncClientPeriodInSeconds(5 * 60);
        Assert.assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void testShouldSyncForNotNullLastSynced() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        Assert.assertTrue(rpSyncService.shouldSync(rp));
    }

    private static OxdServerConfiguration parseConfiguration(String pathToYaml) throws IOException, ConfigurationException {

        File file = new File(pathToYaml);
        if (!file.exists()) {
            System.out.println("Failed to find yml configuration file. Please check " + pathToYaml);
            System.exit(1);
        }

        DefaultConfigurationFactoryFactory<OxdServerConfiguration> configurationFactoryFactory = new DefaultConfigurationFactoryFactory<>();
        ConfigurationFactory<OxdServerConfiguration> configurationFactory = configurationFactoryFactory.create(OxdServerConfiguration.class, Validators.newValidatorFactory().getValidator(), Jackson.newObjectMapper(), "dw");
        return configurationFactory.build(file);
    }
}
