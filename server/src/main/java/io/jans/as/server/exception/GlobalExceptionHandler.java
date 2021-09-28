/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.exception;

import io.jans.as.server.model.exception.InvalidSessionStateException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import java.util.Iterator;

/**
 * Created by eugeniuparvan on 8/29/17.
 */
public class GlobalExceptionHandler extends ExceptionHandlerWrapper {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExceptionHandler wrapped;

    GlobalExceptionHandler(ExceptionHandler exception) {
        this.wrapped = exception;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return this.wrapped;
    }

    public void handle() throws FacesException {
        final Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();

        while (i.hasNext()) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();

            Throwable t = context.getException();
            final FacesContext fc = FacesContext.getCurrentInstance();
            final ExternalContext externalContext = fc.getExternalContext();
            try {
				if (isInvalidSessionStateException(t)) {
	                log.error(t.getMessage(), t);
					performRedirect(externalContext, "/error_session.htm");
				} else {
	                log.error(t.getMessage(), t);
	                performRedirect(externalContext, "/error_service.htm");
				}
                fc.renderResponse();
            } finally {
                i.remove();
            }
        }
        getWrapped().handle();
    }

    private boolean isInvalidSessionStateException(Throwable t) {
        return ExceptionUtils.getRootCause(t) instanceof InvalidSessionStateException;
    }

    private void performRedirect(ExternalContext externalContext, String viewId) {
        try {
            externalContext.redirect(externalContext.getRequestContextPath() + viewId);
        } catch (Exception e) {
            log.error("Can't perform redirect to viewId: " + viewId, e);
        }
    }
}