package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.testing.ResourceHelpers;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.server.RegisterSiteTest;
import org.gluu.oxd.server.TestUtils;
import org.gluu.oxd.server.Tester;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.guice.GuiceModule;
import org.gluu.oxd.server.mapper.RegisterResponseMapper;
import org.gluu.oxd.server.persistence.service.PersistenceService;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.testng.AssertJUnit.*;

@Guice(modules = GuiceModule.class)
public class RpSyncServiceTest {

    @Inject
    ConfigurationService configurationService;
    @Inject
    RpService rpService;
    @Inject
    RpSyncService rpSyncService;
    @Inject
    PersistenceService persistenceService;

    @BeforeClass
    public void setUp() throws IOException, ConfigurationException {
        configurationService.setConfiguration(TestUtils.parseConfiguration(ResourceHelpers.resourceFilePath("oxd-server-jenkins.yml")));
        persistenceService.create();
        rpService.removeAllRps();
        rpService.load();
    }

    @AfterSuite
    public void tearDownSuite() {
        persistenceService.destroy();
    }

    @Test
    public void forNullRp_shouldNotSync() {
        assertFalse(rpSyncService.shouldSync(null));
        rpService.removeAllRps();
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

    @Parameters({"host", "opHost", "redirectUrls", "logoutUrl", "postLogoutRedirectUrls"})
    @Test
    public void testRpGrantTypesSync(String host, String opHost, String redirectUrls, String postLogoutRedirectUrls, String logoutUrl) throws IOException {
        RegisterSiteResponse resp = RegisterSiteTest.registerSite(Tester.newClient(host), opHost, redirectUrls, logoutUrl, postLogoutRedirectUrls, true);
        Rp oxdRpBeforeSync = rpService.getRps().get(resp.getOxdId());//grant_types: [authorization_code, uma-ticket, client_credentials]
        final RegisterResponse response = rpSyncService.readClientFromRp(resp.getClientRegistrationClientUri(), resp.getClientRegistrationAccessToken());

        Rp opClientRp = RegisterResponseMapper.createRp(response);//grant_types: [refresh_token, implicit, authorization_code, uma-ticket, client_credentials]
        Rp oxdRpAfterSync = rpSyncService.getRpTest(resp.getOxdId());//grant_types: [refresh_token, implicit, authorization_code, uma-ticket, client_credentials]
        //grant_types of Rp before sync are not equal to grant_types of client at OP
        assertNotSame(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(oxdRpBeforeSync.getGrantType())), Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(opClientRp.getGrantType())));
        //grant_types of Rp after sync are equal to grant_types of client at OP
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(oxdRpAfterSync.getGrantType())), Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(opClientRp.getGrantType())));
    }
}
