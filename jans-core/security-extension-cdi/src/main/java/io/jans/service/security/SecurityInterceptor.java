/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.security;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.jans.service.el.ExpressionEvaluator;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@Interceptor
@InterceptSecure({})
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class SecurityInterceptor implements Serializable {

    private static final long serialVersionUID = 8227941471496200128L;

    @Inject
    private Logger log;

    @Inject
    private SecurityExtension securityExtension;

    @Inject
    private ExpressionEvaluator expressionEvaluator;

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        InterceptSecure is = securityExtension.getInterceptSecure(ctx.getMethod());

        // SecurityChecking restrictions
        Secure[] constraints = (is == null) ? new Secure[0] : is.value();

        // Getting the parameter values
        Map<String, Object> secureVars = computeParameterValues(ctx);

        for (Secure constraint : constraints) {
            Boolean expressionValue = expressionEvaluator.evaluateValueExpression(constraint.value(), Boolean.class, secureVars);

            if ((expressionValue == null) || !expressionValue) {
                log.debug("Method: '{}' constrain '{}' evaluation is null or false!", ctx.getMethod(), constraint);
                throw new SecurityEvaluationException();
            }
        }

        return ctx.proceed();
    }

    private Map<String, Object> computeParameterValues(InvocationContext ctx) {
        Annotation[][] parametersAnnotations = ctx.getMethod().getParameterAnnotations();
        Map<String, Object> secureVariables = new HashMap<String, Object>();
        for (int i = 0; i < parametersAnnotations.length; i++) {
            Annotation[] parameterAnnotations = parametersAnnotations[i];
            for (Annotation parameterAnnotation : parameterAnnotations) {
                if (SecureVariable.class.isAssignableFrom(parameterAnnotation.annotationType())) {
                    SecureVariable secureVariable = (SecureVariable) parameterAnnotation;
                    Object paramValue = ctx.getParameters()[i];
                    secureVariables.put(secureVariable.value(), paramValue);
                }
            }
        }

        return secureVariables;
    }

}
