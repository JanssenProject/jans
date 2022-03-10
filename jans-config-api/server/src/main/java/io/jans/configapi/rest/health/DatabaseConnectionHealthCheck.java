/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.health;

import io.jans.configapi.service.auth.ConfigurationService;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class DatabaseConnectionHealthCheck implements HealthCheck {

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("jans-config-api readiness");
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
