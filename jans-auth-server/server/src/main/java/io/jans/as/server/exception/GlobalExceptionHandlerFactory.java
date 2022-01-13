/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.exception;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

/**
 * Created by eugeniuparvan on 8/29/17.
 */
public class GlobalExceptionHandlerFactory extends ExceptionHandlerFactory {
    private final ExceptionHandlerFactory exceptionHandlerFactory;

    public GlobalExceptionHandlerFactory(ExceptionHandlerFactory exceptionHandlerFactory) {
        this.exceptionHandlerFactory = exceptionHandlerFactory;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new GlobalExceptionHandler(exceptionHandlerFactory.getExceptionHandler());
    }
}