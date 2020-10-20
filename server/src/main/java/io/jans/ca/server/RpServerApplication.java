package io.jans.ca.server;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpServerApplication extends Application<RpServerConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(RpServerApplication.class);

    public static void main(String[] args) {
        try {
            if (args.length > 0 && "stop".equalsIgnoreCase(args[0])) {
                ServerLauncher.shutdown(true);
                return;
            } else {
                new RpServerApplication().run(args);
            }
        } catch (Throwable e) {
            if (args.length > 0 && "stop".equalsIgnoreCase(args[0])) {
                LOG.error("Failed to stop jans_client_api.", e);
            } else {
                LOG.error("Failed to start jans_client_api.", e);
            }
            System.exit(1);
        }
    }

    @Override
    public void initialize(Bootstrap<RpServerConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(RpServerConfiguration configuration, Environment environment) {
        ServerLauncher.configureServices(configuration);
        TracingUtil.configureGlobalTracer(configuration, "jans_client_api");
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
