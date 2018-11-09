package org.xdi.service.exception;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

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
