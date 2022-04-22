/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.exception;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception handler extension
 * 
 * @author Yuriy Movchan Date: 05/22/2017
 */
@ApplicationScoped
public class ExceptionHandlerExtension implements Extension {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerExtension.class.getName());

    private Map<Class<? extends Throwable>, List<Method>> allExceptionHandlers = new HashMap<Class<? extends Throwable>, List<Method>>();

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
        final AnnotatedTypeConfigurator<X> cat = pat.configureAnnotatedType();

        // Collect the ExceptionHandler annotations from the methods
        List<ExceptionHandler> typeExceptionHandlerAnnotations = new ArrayList<ExceptionHandler>();
        for (AnnotatedMethodConfigurator<? super X> methodConfiguration : cat.methods()) {
            final AnnotatedMethod<?> method = methodConfiguration.getAnnotated();

            if (method.getJavaMember().getExceptionTypes().length != 0) {
                log.trace("Handler method '{}' must not throw exceptions. Ignoring it.", method.getJavaMember());
                continue;
            }

            final List<ExceptionHandler> methodAnnotations = new ArrayList<ExceptionHandler>();

            collectAnnotations(ExceptionHandler.class, method, methodAnnotations);

            // Store in the map if we find Secure annotations
            for (ExceptionHandler eh : methodAnnotations) {
                for (Class<? extends Throwable> ex : eh.value()) {
                    List<Method> handlerMethods = allExceptionHandlers.get(ex);
                    if (handlerMethods == null) {
                        handlerMethods = new ArrayList<Method>();
                        allExceptionHandlers.put(ex, handlerMethods);
                    }
                    handlerMethods.add(method.getJavaMember());
                }
            }
        }
    }

    private <T> void collectAnnotations(Class<T> annotationType, AnnotatedMethod<?> method, List<T> values) {
        for (Annotation annotation : method.getAnnotations()) {
            collectAnnotations(annotationType, annotation, values);
        }
    }

    private <T> void collectAnnotations(Class<T> annotationType, Annotation annotation, List<T> values) {
        if (annotationType.isAssignableFrom(annotation.annotationType())) {
            values.add((T) annotation);
        }
    }

    public Map<Class<? extends Throwable>, List<Method>> getAllExceptionHandlers() {
        return allExceptionHandlers;
    }

}
