package org.xdi.oxauth.service.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.weld.context.bound.BoundRequestContext;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Weld implementation. Associated storage is put in
 * volatile job execution context data map.
 * 
 * @author Martin Kouba
 */
public class WeldBoundRequestJobListener extends BoundRequestJobListener {

	private static final Logger logger = LoggerFactory
			.getLogger(WeldBoundRequestJobListener.class);

	@Inject
	private BoundRequestContext requestContext;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cz.symbiont_it.cdiqi.spi.BoundRequestJobListener#startRequest(org.quartz
	 * .JobExecutionContext)
	 */
	protected void startRequest(JobExecutionContext context) {

		Map<String, Object> requestDataStore = Collections
				.synchronizedMap(new HashMap<String, Object>());
		context.put(REQUEST_DATA_STORE_KEY, requestDataStore);

		requestContext.associate(requestDataStore);
		requestContext.activate();

		logger.debug("Bound request started");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cz.symbiont_it.cdiqi.spi.BoundRequestJobListener#endRequest(org.quartz
	 * .JobExecutionContext)
	 */
	@SuppressWarnings("unchecked")
	protected void endRequest(JobExecutionContext context) {

		try {

			requestContext.invalidate();
			requestContext.deactivate();

		} finally {
			requestContext.dissociate((Map<String, Object>) context
					.get(REQUEST_DATA_STORE_KEY));
		}
		logger.debug("Bound request ended");
	}

}
