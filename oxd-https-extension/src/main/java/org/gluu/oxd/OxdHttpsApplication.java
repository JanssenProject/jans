package org.gluu.oxd;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class OxdHttpsApplication extends Application<OxdHttpsConfiguration> {

    public static void main(String[] args) throws Exception {
        new OxdHttpsApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<OxdHttpsConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(OxdHttpsConfiguration configuration, Environment environment) {
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        });
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new RestResource(configuration));
    }
}
