package org.gluu.oxd.server;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OxdServerApplication extends Application<OxdServerConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(OxdServerApplication.class);

    public static void main(String[] args) {
        try {
            new OxdServerApplication().run(args);
        } catch (Throwable e) {
            LOG.error("Failed to start oxd-server.", e);
            System.exit(1);
        }
    }

    @Override
    public void initialize(Bootstrap<OxdServerConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(OxdServerConfiguration configuration, Environment environment) {
        ServerLauncher.configureServices(configuration);
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        });
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new RestResource());
    }
}
