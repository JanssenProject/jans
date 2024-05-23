package io.jans.lock.service.provider.metric;

import org.slf4j.Logger;

import io.jans.service.cdi.qualifier.Implementation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Null metric provider
 *
 * @author Yuriy Movchan Date: 05/23/2024
 */
@Implementation
@ApplicationScoped
public class NullMetricProvider extends MetricProvider {
	
	public static String METRIC_PROVIDER_TYPE = "DISABLED";

	@Inject
	private Logger log;

	@Override
	public void destroy() {
		log.debug("Destroy metric provider");
	}

}
