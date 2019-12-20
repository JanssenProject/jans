package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.testing.ResourceHelpers;
import org.gluu.oxd.server.TestUtils;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.guice.GuiceModule;
import org.gluu.oxd.server.persistence.PersistenceService;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

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
        configurationService.setConfiguration(TestUtils.parseConfiguration(ResourceHelpers.resourceFilePath("oxd-server-jenkins.yml")));
        persistenceService.create();
        service.removeAllRps();
        service.load();
    }

    @AfterSuite
    public void tearDownSuite() {
        persistenceService.destroy();
    }

    @Test
    public void forNullRp_shouldNotSync() {
        assertFalse(rpSyncService.shouldSync(null));
        service.removeAllRps();
    }

    @Test
    public void forNotNullRp_shouldSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void forSyncAllowed_shouldSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void forSyncNotAllowed_shouldNotSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(false);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        assertFalse(rpSyncService.shouldSync(rp));
    }

    @Test
    public void forNoLastSyncedDate_shouldSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        rp.setSyncClientPeriodInSeconds(5 * 60);
        assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void forLastSyncedDatePresent_shouldSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void afterSyncClientPeriodfromLastSynced_shouldSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(5 * 60);
        assertTrue(rpSyncService.shouldSync(rp));
    }

    @Test
    public void beforeSyncClientPeriodfromLastSynced_shouldNotSync() {
        Rp rp = new Rp();
        rp.setSyncClientFromOp(true);
        //adding date 10 mintutes behind current time and setting as LastSynced
        rp.setLastSynced(Utils.addTimeToDate(new Date(), -10, Calendar.MINUTE));
        rp.setSyncClientPeriodInSeconds(15 * 60);
        assertFalse(rpSyncService.shouldSync(rp));
    }
}
