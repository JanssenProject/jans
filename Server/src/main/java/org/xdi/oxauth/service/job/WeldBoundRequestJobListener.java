package org.xdi.oxauth.service.job;

//import org.jboss.weld.context.bound.BoundRequestContext;
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

	@Override
	protected void startRequest(JobExecutionContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void endRequest(JobExecutionContext context) {
		// TODO Auto-generated method stub
		
	}

//	@Inject
//	private BoundRequestContext requestContext;
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see
//	 * cz.symbiont_it.cdiqi.spi.BoundRequestJobListener#startRequest(org.quartz
//	 * .JobExecutionContext)
//	 */
//	protected void startRequest(JobExecutionContext context) {
//
//		Map<String, Object> requestDataStore = Collections
//				.synchronizedMap(new HashMap<String, Object>());
//		context.put(REQUEST_DATA_STORE_KEY, requestDataStore);
//
//		requestContext.associate(requestDataStore);
//		requestContext.activate();
//
//		logger.debug("Bound request started");
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see
//	 * cz.symbiont_it.cdiqi.spi.BoundRequestJobListener#endRequest(org.quartz
//	 * .JobExecutionContext)
//	 */
//	@SuppressWarnings("unchecked")
//	protected void endRequest(JobExecutionContext context) {
//
//		try {
//
//			requestContext.invalidate();
//			requestContext.deactivate();
//
//		} finally {
//			requestContext.dissociate((Map<String, Object>) context
//					.get(REQUEST_DATA_STORE_KEY));
//		}
//		logger.debug("Bound request ended");
//	}

}
