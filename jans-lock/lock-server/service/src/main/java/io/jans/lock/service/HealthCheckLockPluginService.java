package io.jans.lock.service;

import io.jans.lock.service.config.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.HealthCheckPluginService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Health check plugin service
 *
 * @author Yuriy Movchan Date: 24/10/2024
 */
@ApplicationScoped
public class HealthCheckLockPluginService implements HealthCheckPluginService {

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Override
	public String provideHealthCheckData() {
    	boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
    	String dbStatus = isConnected ? "online" : "offline"; 
        return "{\"status\": \"running\", \"db_status\":\"" + dbStatus + "\"}";
	}

	@Override
	public String provideServiceName() {
		return ConfigurationFactory.DOCUMENT_STORE_MANAGER_JANS_LOCK_TYPE;
	}

}
