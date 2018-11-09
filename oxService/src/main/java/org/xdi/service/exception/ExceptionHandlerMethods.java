package org.xdi.service.exception;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

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
