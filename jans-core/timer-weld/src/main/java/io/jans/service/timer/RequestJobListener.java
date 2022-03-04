/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.weld.context.bound.BoundRequestContext;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan Date: 04/04/2017
 */
@Dependent
public class RequestJobListener implements JobListener {

    @Inject
    private Logger log;

    @Inject
    private BoundRequestContext requestContext;

    protected static final String REQUEST_DATA_STORE_KEY = RequestJobListener.class.getName() + "_REQUEST_DATA_STORE_KEY";

    public String getName() {
        return getClass().getName();
    }

    public void jobToBeExecuted(JobExecutionContext context) {
        startRequest(context);
    }

    public void jobExecutionVetoed(JobExecutionContext context) {
        endRequest(context);
    }

    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        endRequest(context);
    }

    protected void startRequest(JobExecutionContext context) {
        Map<String, Object> requestDataStore = Collections.synchronizedMap(new HashMap<String, Object>());
        context.put(REQUEST_DATA_STORE_KEY, requestDataStore);

        requestContext.associate(requestDataStore);
        requestContext.activate();

        log.debug("Bound request started");
    }

    protected void endRequest(JobExecutionContext context) {
        try {
            requestContext.invalidate();
            requestContext.deactivate();
        } finally {
            requestContext.dissociate((Map<String, Object>) context.get(REQUEST_DATA_STORE_KEY));
        }
        log.debug("Bound request ended");
    }

}
