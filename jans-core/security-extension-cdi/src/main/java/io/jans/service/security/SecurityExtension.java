/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import jakarta.enterprise.util.AnnotationLiteral;

/**
 * Security extension
 *
 * @author Yuriy Movchan Date: 05/22/2017
 */
public class SecurityExtension implements Extension {

    private Map<Method, InterceptSecure> interceptSecureForMethods = new HashMap<Method, InterceptSecure>();

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
        // Wrap this to override the annotations of the class
        final AnnotatedType<X> at = pat.getAnnotatedType();
        final AnnotatedTypeConfigurator<X> cat = pat.configureAnnotatedType();

        // Collect Secure annotation from the type
        List<Secure> typeSecureAnnotations = new ArrayList<Secure>();
        for (Annotation annotation : pat.getAnnotatedType().getAnnotations()) {
            collectAnnotations(Secure.class, annotation, typeSecureAnnotations);
        }

        // Collect the Secure annotations from the methods
        for (AnnotatedMethodConfigurator<? super X> methodConfiguration : cat.methods()) {
            AnnotatedMethod<?> method = methodConfiguration.getAnnotated();
            final List<Secure> methodAnnotations = new ArrayList<Secure>(typeSecureAnnotations);

            collectAnnotations(Secure.class, method, methodAnnotations);

            // Store in the map if we find Secure annotations
            if (methodAnnotations.size() > 0) {
                InterceptSecure is = new InterceptSecureImpl(methodAnnotations.toArray(new Secure[methodAnnotations.size()]));

                // Add InterceptSecure annotation
                methodConfiguration.add(is);

                interceptSecureForMethods.put(method.getJavaMember(), is);
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

    public InterceptSecure getInterceptSecure(Method method) {
        return interceptSecureForMethods.get(method);
    }

    private static class InterceptSecureImpl extends AnnotationLiteral<InterceptSecure> implements InterceptSecure {

        private final Secure[] values;

        InterceptSecureImpl(Secure[] values) {
            this.values = values;
        }

        @Override
        public Secure[] value() {
            return values;
        }
    }

}
