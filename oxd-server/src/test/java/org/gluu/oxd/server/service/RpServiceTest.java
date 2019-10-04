package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.testing.ResourceHelpers;
import org.assertj.core.util.Lists;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.guice.GuiceModule;
import org.gluu.oxd.server.persistence.PersistenceService;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

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
        configurationService.setConfiguration(parseConfiguration(ResourceHelpers.resourceFilePath("oxd-server-jenkins.yml")));
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

    @Test (enabled = false)
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

        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");
    }

    @Test
    public void remove() throws Exception {
        Rp rp = newRp();

        service.create(rp);
        assertNotNull(persistenceService.getRp(rp.getOxdId()));

        rp.setClientName("Updated name");
        service.update(rp);

        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");

        service.remove(rp.getOxdId());
        try {
            rp = persistenceService.getRp(rp.getOxdId());
            validationService.validate(rp);
            throw new AssertionError("RP is not removed.");
        } catch (HttpException e) {
            assertEquals(e.getCode(), ErrorResponseCode.INVALID_OXD_ID);
        }
    }

    @Test(invocationCount = 10, threadPoolSize = 10, enabled = false)
    public void stressTest() throws IOException {

        final Rp rp = configurationService.defaultRp();
        rp.setOxdId(UUID.randomUUID().toString());
        rp.setPat(UUID.randomUUID().toString());

        service.create(rp);

        for (int i = 0; i < 11; i++) {
            EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        rp.setPat(UUID.randomUUID().toString());
                        service.update(rp);
                        System.out.println("Updated PAT: " + rp.getPat() + ", for site: " + rp.getOxdId());
                    } catch (Throwable e) {
                        throw new AssertionError("Failed to update configuration: " + rp.getOxdId());
                    }
                }
            });
        }
    }

    @Test
    public void testNullFieldsAreSkipped() throws IOException {
        Rp rp = newRp();
        String expectedJson = "{\"oxd_id\":\""+rp.getOxdId()+"\",\"op_host\":\"test.gluu.org\",\"response_types\":[\"code\"],\"scope\":[\"openid\",\"profile\",\"email\"],\"ui_locales\":[\"en\"],\"claims_locales\":[\"en\"],\"acr_values\":[\"\"],\"access_token_as_jwt\":false,\"rpt_as_jwt\":false,\"front_channel_logout_session_required\":false,\"run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims\":false,\"require_auth_time\":false,\"trusted_client\":false}";
        assertEquals(Jackson2.createRpMapper().readTree(expectedJson), Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp)));
    }

    @Test
    public void testNonNullFieldsAreAlwaysPresent() throws IOException {
        Rp rp = createRpWithNotNullFields();
        //here make sure expectedJson contains all non null values, to make sure serialization is correct.
        String expectedJson = "{\"oxd_id\":\"test_oxd_id\",\"op_host\":\"https://test.gluu.org\",\"redirect_uri\":\"https://localhost:5053/authorization\",\"application_type\":\"web\",\"redirect_uris\":[\"https://localhost:5053/authorization\",\"https://localhost:5053/authorization/page1\",\"https://localhost:5053/authorization/page2\"],\"response_types\":[\"code\"],\"client_id\":\"test_client_id\",\"client_secret\":\"test_client_secret\",\"client_registration_access_token\":\"test_client_registration_access_token\",\"client_registration_client_uri\":\"https://test.gluu.org/oxauth/restv1/register?client_id=test_client_id\",\"scope\":[\"openid\",\"profile\",\"email\"],\"ui_locales\":[\"en\"],\"claims_locales\":[\"en\"],\"acr_values\":[\"\"],\"access_token_as_jwt\":false,\"rpt_as_jwt\":false,\"front_channel_logout_session_required\":false,\"run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims\":false,\"require_auth_time\":false,\"trusted_client\":false}";
        assertEquals(Jackson2.createRpMapper().readTree(expectedJson), Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp)));
    }

    public Rp newRp() throws IOException {
        Rp rp = new Rp(configurationService.defaultRp());
        rp.setOxdId(UUID.randomUUID().toString());
        rp.setOpHost("test.gluu.org");
        return rp;
    }

    public Rp createRpWithNotNullFields() throws IOException {
        Rp rp = new Rp(configurationService.defaultRp());
        rp.setOxdId("test_oxd_id");
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
