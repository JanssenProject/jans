package org.xdi.oxauth.exception;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

/**
 * Created by eugeniuparvan on 8/29/17.
 */
public class GlobalExceptionHandlerFactory extends ExceptionHandlerFactory {
    private ExceptionHandlerFactory exceptionHandlerFactory;

    public GlobalExceptionHandlerFactory(ExceptionHandlerFactory exceptionHandlerFactory) {
        this.exceptionHandlerFactory = exceptionHandlerFactory;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new GlobalExceptionHandler(exceptionHandlerFactory.getExceptionHandler());
    }
}