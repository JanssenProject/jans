/**
 * Background scheduler application 
 * This application runs and monitors various background jobs that are 
 * used for the proper operation of Janssen and Keycloak 
 * E.g. configuration synchronization 
 * Author : Rolain Djeumen <rolain@gluu.org> 
 */

package io.jans.kc.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import io.jans.kc.scheduler.config.ConfigApiAuthnMethod;
import io.jans.kc.scheduler.config.AppConfiguration;
import io.jans.kc.scheduler.config.AppConfigException;

import io.jans.kc.scheduler.job.*;
import io.jans.kc.scheduler.job.JobScheduler;
import io.jans.kc.scheduler.job.JobSchedulerException;
import io.jans.kc.scheduler.job.impl.QuartzJobScheduler;
import io.jans.kc.api.config.client.ApiCredentials;
import io.jans.kc.api.config.client.ApiCredentialsProvider;
import io.jans.kc.api.config.client.impl.CredentialsProviderError;
import io.jans.kc.api.config.client.*;
import io.jans.kc.api.config.client.impl.*;

import io.jans.kc.api.admin.client.*;
import io.jans.kc.api.admin.client.model.AuthenticationFlow;
import io.jans.kc.api.admin.client.model.ManagedSamlClient;
import io.jans.saml.metadata.parser.ParserCreateError;
import io.jans.saml.metadata.util.SAXUtils;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class App {
    
    private static final String APP_DISPLAY_NAME = "Keycloak";
    private static final String PROP_APP_CFG_FILE = "app.config";
    private static final String DEFAULT_APP_CFG_FILEPATH = "/opt/kc-scheduler/conf/config.properties";
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static AppConfiguration config = null;
    private static JobScheduler jobScheduler = null;
    private static JansConfigApiFactory jansConfigApiFactory = null;
    private static KeycloakApiFactory keycloakApiFactory = null;

    private static boolean running = false;
    private static boolean isCronJob = true;
    /*
     * Entry point 
     */
    public static void main(String[] args) throws InterruptedException, ParserCreateError, ParserConfigurationException, SAXException {

        log.info("Application starting ...");
        try {
            log.info("Loading application configuration");
            config = loadApplicationConfiguration();
            log.info("Application configuration loaded successfully. {}",config.toString());


            log.info("Setting up access to external apis");
            jansConfigApiFactory = JansConfigApiFactory.createFactory(config);
            keycloakApiFactory = KeycloakApiFactory.createFactory(config);

            //initialize application objects
            log.info("Initialization additional application objects");
            SAXUtils.init();

            if(isCronJob) {
                log.info("Running as cron, skiping scheduler initialization");
                runCronJobs();
                log.info("Jobs run to completion.");
            }else {
                log.info("Not running as cron job. Initializing scheduler");
                jobScheduler = createJobScheduler(config);
                startJobScheduler(jobScheduler);
                log.info("Starting jans trust relationship sync job");
                startJansTrustRelationshipSyncJob(config);
                log.info("Performing post-startup operations");
                performPostStartupOperations();
                log.info("Application startup successful");
                while(running) {
                    Thread.sleep(1000);
                }
            }
            log.info("Application shutting down");
        }catch(StartupError e) {
            log.error("Application startup failed",e);
            if(jobScheduler != null) {
                jobScheduler.stop();
            }
            System.exit(-1);
            return;
        }catch(InterruptedException e) {
            log.error("Application interrupted",e);
            Thread.currentThread().interrupt();
        }catch(Exception e) {
            log.error("Fatal error starting application",e);
            if(jobScheduler != null ) {
                jobScheduler.stop();
            }
            System.exit(-1);
        }

    }

    private static final String getAppConfigFileName() {

        return System.getProperty(PROP_APP_CFG_FILE);
    }

    private static final AppConfiguration loadApplicationConfiguration() {

        try {
            String config_file_name = getAppConfigFileName();
            if(config_file_name == null) {
                log.debug("No application configuration specified in environment variable. Using default");
                config_file_name =  DEFAULT_APP_CFG_FILEPATH;
            }
            log.debug("Application configuration file: {} ",config_file_name);
            return AppConfiguration.fromFile(config_file_name);
        }catch(AppConfigException e) {
            throw new StartupError("Application startup failed",e);
        }
    }

    private static final JobScheduler createJobScheduler(AppConfiguration configuration) {

        return createQuartzJobSchedulerFromConfiguration(configuration);
    }

    private static final void startJobScheduler(JobScheduler jobScheduler) {

        jobScheduler.start();
    }

    private static final void startJansTrustRelationshipSyncJob(AppConfiguration configuration) {

        try {

            if(configuration.trustRelationshipSyncScheduleInterval() == null) {
                throw new StartupError("Missing tr sync job scheduling interval.");
            }
            RecurringJobSpec jobspec = RecurringJobSpec.builder()
                .jobClass(TrustRelationshipSyncJob.class)
                .name(TrustRelationshipSyncJob.class.getSimpleName())
                .schedulingInterval(configuration.trustRelationshipSyncScheduleInterval())
                .build();
        
            jobScheduler.scheduleRecurringJob(jobspec);
        }catch(AppConfigException e) {
            throw new StartupError("Failed to start TR sync job",e);
        }
    }

    private static final JobScheduler createQuartzJobSchedulerFromConfiguration(AppConfiguration config) {
        
        try {
            return QuartzJobScheduler.builder()
                .name(config.quatzSchedulerName())
                .instanceId(config.quartzSchedulerInstanceId())
                .threadPoolSize(config.quartzSchedulerThreadPoolSize())
                .build();
        }catch(AppConfigException e) {
            throw new StartupError("Could not create quartz job scheduler",e);
        }
    }

    private static final void runCronJobs() {

        TrustRelationshipSyncJob trsyncjob = new TrustRelationshipSyncJob();
        trsyncjob.run(null);
    }

    private static final void performPostStartupOperations() {

        running = true;
        registerShutdownHook();
    }

    private static final void registerShutdownHook() {

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public static final KeycloakApi keycloakApi() {

        return keycloakApiFactory.newApiClient();
    }

    public static final JansConfigApi jansConfigApi() {

        return jansConfigApiFactory.newApiClient();
    }

    public static final AppConfiguration configuration() {

        return config;
    }
    
    private static class KeycloakApiFactory {

        private final KeycloakConfiguration kcConfig;

        private KeycloakApiFactory(KeycloakConfiguration kcConfig) {

            this.kcConfig = kcConfig;
        }

        public KeycloakApi newApiClient() {

            return KeycloakApi.createInstance(kcConfig);
        }

        public static KeycloakApiFactory createFactory(AppConfiguration config) {

            try {
                KeycloakConfiguration cfg = KeycloakConfiguration.fromAppConfiguration(config);
                return new KeycloakApiFactory(cfg);
            }catch(KeycloakConfigurationError e) {
                throw new StartupError("Could not initialize keycloak API",e);
            }
        }
    }

    private static class JansConfigApiFactory {

        private String endpoint;
        private ApiCredentialsProvider credsprovider;

        private JansConfigApiFactory(String endpoint, ApiCredentialsProvider credsprovider) {

            this.endpoint = endpoint;
            this.credsprovider = credsprovider;
        }

        public JansConfigApi newApiClient() {

            return JansConfigApi.createInstance(endpoint,credsprovider.getApiCredentials());
        }

        public static JansConfigApiFactory createFactory(AppConfiguration config) {
            try {
                TokenEndpointAuthnParams authparams = null;
                if(config.configApiAuthMethod() == ConfigApiAuthnMethod.BASIC_AUTHN) {
                    authparams = TokenEndpointAuthnParams.basicAuthn(
                        config.configApiAuthClientId(),
                        config.configApiAuthClientSecret(),
                        config.configApiAuthScopes());
                }else if(config.configApiAuthMethod() == ConfigApiAuthnMethod.POST_AUTHN) {
                    authparams = TokenEndpointAuthnParams.postAuthn(
                        config.configApiAuthClientId(),
                        config.configApiAuthClientSecret(),
                        config.configApiAuthScopes()
                    );
                }else {
                    throw new StartupError("Could not initialize jans-config API. Unsupported authn method");
                }
                ApiCredentialsProvider provider = OAuthApiCredentialsProvider.create(config.configApiAuthUrl(),authparams);
                return new JansConfigApiFactory(config.configApiUrl(),provider);

            }catch(CredentialsProviderError e) {
                throw new StartupError("Could not initialize jans-config API",e);
            }catch(TokenEndpointAuthnParamError e) {
                throw new StartupError("Could not initialize jans-config API",e);
            }
        }
    }
    
    public static class ShutdownHook extends Thread  {
        
        
        @Override
        public void run() {

            try {
                log.info("Shutting down application");
                if (jobScheduler != null) {
                    jobScheduler.stop();
                }
            }catch(Exception e) {
                log.warn("Something unexpected happened while stopping the scheduler",e);
            }

            running = false;
        }
    }

    public static class StartupError extends RuntimeException {

        public StartupError(String msg) {
            super(msg);
        }

        public StartupError(String msg, Throwable cause) {
            super(msg,cause);
        }
    }
}
