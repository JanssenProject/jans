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
            if (args.length > 0 && "stop".equalsIgnoreCase(args[0])) {
                ServerLauncher.shutdown(true);
                return;
            } else {
                new OxdServerApplication().run(args);
            }
        } catch (Throwable e) {
            if (args.length > 0 && "stop".equalsIgnoreCase(args[0])) {
                LOG.error("Failed to stop oxd-server.", e);
            } else {
                LOG.error("Failed to start oxd-server.", e);
            }
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
        TracingUtil.configureGlobalTracer(configuration, "oxd-server");
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() {
                return Result.healthy();
            }
        });
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(RestResource.class);
    }
}
