package org.gluu.configapi.rest.health;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class ApiHealthCheck implements HealthCheck {
	public HealthCheckResponse call() {
		return HealthCheckResponse.up("oxauth-config-api liveness");
	}
}
