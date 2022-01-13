package io.jans.ca.server.service;

import com.google.inject.Inject;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.testing.ResourceHelpers;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.TestUtils;
import io.jans.ca.server.guice.GuiceModule;
import io.jans.ca.server.persistence.service.PersistenceService;
import org.testng.annotations.*;
import org.testng.collections.Lists;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */
@Guice(modules = GuiceModule.class)
public class RpServiceTest {

    private static ExecutorService EXECUTOR_SERVICE;

    @Inject
    ConfigurationService configurationService;
    @Inject
    RpService service;
    @Inject
    PersistenceService persistenceService;
    @Inject
    ValidationService validationService;

    @BeforeClass
    public void setUp() throws IOException, ConfigurationException {
        configurationService.setConfiguration(TestUtils.parseConfiguration(ResourceHelpers.resourceFilePath("client-api-server-jenkins.yml")));
        persistenceService.create();
        service.removeAllRps();
        service.load();
    }

    @BeforeSuite
    public void setUpSuite() {
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(200);
    }

    @AfterSuite
    public void tearDownSuite() {
        service.removeAllRps();
        persistenceService.destroy();
        EXECUTOR_SERVICE.shutdown();
    }

    @Test(enabled = false)
    public void load() {
        assertEquals(service.getRps().size(), 1);
    }

    @Test
    public void persist() throws Exception {
        Rp rp = newRp();

        service.create(rp);
        assertEquals(service.getRps().size(), 1);

        rp.setClientName("Updated name");
        service.update(rp);

        assertEquals(persistenceService.getRp(rp.getRpId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getRpId()).getClientName(), "Updated name");
    }

    @Test
    public void remove() throws Exception {
        Rp rp = newRp();

        service.create(rp);
        assertNotNull(persistenceService.getRp(rp.getRpId()));

        rp.setClientName("Updated name");
        service.update(rp);

        assertEquals(persistenceService.getRp(rp.getRpId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getRpId()).getClientName(), "Updated name");

        service.remove(rp.getRpId());
        try {
            rp = persistenceService.getRp(rp.getRpId());
            validationService.validate(rp);
            throw new AssertionError("RP is not removed.");
        } catch (HttpException e) {
            assertEquals(e.getCode(), ErrorResponseCode.INVALID_RP_ID);
        }
    }

    @Test(invocationCount = 10, threadPoolSize = 10, enabled = false)
    public void stressTest() throws IOException {

        final Rp rp = configurationService.defaultRp();
        rp.setRpId(UUID.randomUUID().toString());
        rp.setPat(UUID.randomUUID().toString());

        service.create(rp);

        for (int i = 0; i < 11; i++) {
            EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        rp.setPat(UUID.randomUUID().toString());
                        service.update(rp);
                        System.out.println("Updated PAT: " + rp.getPat() + ", for site: " + rp.getRpId());
                    } catch (Throwable e) {
                        throw new AssertionError("Failed to update configuration: " + rp.getRpId());
                    }
                }
            });
        }
    }

    @Test
    public void testNullFieldsAreSkipped() throws IOException {
        Rp rp = newRp();
        String expectedJson = "{\"rp_id\":\"" + rp.getRpId() + "\",\"op_host\":\"test.gluu.org\",\"response_types\":[\"code\"],\"scope\":[\"openid\",\"profile\",\"email\"],\"ui_locales\":[\"en\"],\"claims_locales\":[\"en\"],\"acr_values\":[\"\"],\"access_token_as_jwt\":false,\"rpt_as_jwt\":false,\"front_channel_logout_session_required\":false,\"run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims\":false,\"require_auth_time\":false,\"allow_spontaneous_scopes\":false,\"sync_client_from_op\":false,\"sync_client_period_in_seconds\":3600}";
        assertEquals(Jackson2.createRpMapper().readTree(expectedJson), Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp)));
    }

    @Test
    public void testNonNullFieldsAreAlwaysPresent() throws IOException {
        Rp rp = createRpWithNotNullFields();
        //here make sure expectedJson contains all non null values, to make sure serialization is correct.
        String expectedJson = "{\"rp_id\":\"test_rp_id\",\"op_host\":\"https://test.gluu.org\",\"redirect_uri\":\"https://localhost:5053/authorization\",\"application_type\":\"web\",\"redirect_uris\":[\"https://localhost:5053/authorization\",\"https://localhost:5053/authorization/page1\",\"https://localhost:5053/authorization/page2\"],\"response_types\":[\"code\"],\"client_id\":\"test_client_id\",\"client_secret\":\"test_client_secret\",\"client_registration_access_token\":\"test_client_registration_access_token\",\"client_registration_client_uri\":\"https://test.gluu.org/oxauth/restv1/register?client_id=test_client_id\",\"scope\":[\"openid\",\"profile\",\"email\"],\"ui_locales\":[\"en\"],\"claims_locales\":[\"en\"],\"acr_values\":[\"\"],\"access_token_as_jwt\":false,\"rpt_as_jwt\":false,\"front_channel_logout_session_required\":false,\"run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims\":false,\"require_auth_time\":false,\"allow_spontaneous_scopes\":false,\"sync_client_from_op\":false,\"sync_client_period_in_seconds\":3600}";
        assertEquals(Jackson2.createRpMapper().readTree(expectedJson), Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp)));
    }

    public Rp newRp() throws IOException {
        Rp rp = new Rp(configurationService.defaultRp());
        rp.setRpId(UUID.randomUUID().toString());
        rp.setOpHost("test.gluu.org");
        return rp;
    }

    public Rp createRpWithNotNullFields() throws IOException {
        Rp rp = new Rp(configurationService.defaultRp());
        rp.setRpId("test_rp_id");
        rp.setOpHost("https://test.gluu.org");
        rp.setRedirectUri("https://localhost:5053/authorization");
        rp.setApplicationType("web");
        rp.setRedirectUris(Lists.newArrayList("https://localhost:5053/authorization",
                "https://localhost:5053/authorization/page1",
                "https://localhost:5053/authorization/page2"));
        rp.setResponseTypes(Lists.newArrayList("code"));
        rp.setClientId("test_client_id");
        rp.setClientSecret("test_client_secret");
        rp.setClientRegistrationAccessToken("test_client_registration_access_token");
        rp.setClientRegistrationClientUri("https://test.gluu.org/oxauth/restv1/register?client_id=test_client_id");
        return rp;
    }
}
