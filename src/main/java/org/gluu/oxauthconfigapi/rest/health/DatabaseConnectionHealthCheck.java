package org.gluu.oxauthconfigapi.rest.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.gluu.oxtrust.service.GroupService;
import org.gluu.persist.PersistenceEntryManager;

@Readiness
@ApplicationScoped
public class DatabaseConnectionHealthCheck implements HealthCheck {

	@Inject
	GroupService groupService;

	@Inject
	PersistenceEntryManager persistenceEntryManager;

	public HealthCheckResponse call() {
		HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("oxauth-config-api readiness");
		try {
			checkDatabaseConnection();
			responseBuilder.up();
		} catch (Exception e) {
			responseBuilder.down().withData("error", e.getMessage());
		}
		return responseBuilder.build();
	}

	private void checkDatabaseConnection() throws Exception {
		groupService.countGroups();
	}

}
