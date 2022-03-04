/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.exception;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;

/**
 * @author Yuriy Movchan Date: 11/05/2018
 */
@Named
public class ExceptionHandlerMethods {

    private Map<Class<? extends Throwable>, List<Method>> allExceptionHandlers;

    public ExceptionHandlerMethods(Map<Class<? extends Throwable>, List<Method>> allExceptionHandlers) {
        this.allExceptionHandlers = allExceptionHandlers;
    }

    public Map<Class<? extends Throwable>, List<Method>> getAllExceptionHandlers() {
        return allExceptionHandlers;
    }

}
