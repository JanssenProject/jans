/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExceptionHandlerProducer {
    @Inject
    private ExceptionHandlerExtension exceptionHandlerExtension;

    @Produces
    @ApplicationScoped
    protected ExceptionHandlerMethods createExceptionHandlerProducer() {
        return new ExceptionHandlerMethods(exceptionHandlerExtension.getAllExceptionHandlers());
    }
}
