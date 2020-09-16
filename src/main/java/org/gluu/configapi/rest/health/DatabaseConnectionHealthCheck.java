package org.gluu.configapi.rest.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.gluu.configapi.service.ConfigurationService;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Readiness
@ApplicationScoped
public class DatabaseConnectionHealthCheck implements HealthCheck {

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("oxauth-config-api readiness");
        try {
            checkDatabaseConnection();
            responseBuilder.up();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            responseBuilder.down().withData("error", e.getMessage());
        }
        return responseBuilder.build();
    }

    private void checkDatabaseConnection() {
        configurationService.findConf();
    }
}
