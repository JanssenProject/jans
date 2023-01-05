/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.oxtrust.auth.uma.annotations;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import io.jans.service.el.ExpressionEvaluator;
import io.jans.service.security.InterceptSecure;
import io.jans.service.security.Secure;
import io.jans.service.security.SecureVariable;
import io.jans.service.security.SecurityEvaluationException;
import io.jans.service.security.SecurityExtension;
import org.slf4j.Logger;

/**
 * Provides service to protect Rest service endpoints with UMA scope.
 * 
 * @author Dmitry Ognyannikov
 */
@Interceptor
@UmaSecure
@Priority(Interceptor.Priority.PLATFORM_AFTER)
@Deprecated
public class UmaSecureInterceptor {

    @Inject
    private Logger log;
    
    @Inject
    private SecurityExtension securityExtension;
    
    @Inject
    private ExpressionEvaluator expressionEvaluator;
    
    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        HttpServletResponse response = null;
        Object[] parameters = ctx.getParameters();
        
        log.trace("REST method call security check. " + ctx.getMethod().getName() + "()");
        
        for (Object parameter : parameters) {
            if (parameter instanceof HttpServletResponse) 
                response = (HttpServletResponse)parameter;
        }
        
    	InterceptSecure is = securityExtension.getInterceptSecure(ctx.getMethod());

        // SecurityChecking  restrictions
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

        try {
            // the method call
            return ctx.proceed();
        } catch (Exception e) {
            log.error("Error calling ctx.proceed in UmaSecureInterceptor");
            // REST call error report
            if (response != null) {
                try { response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR"); } catch (Exception ex) {}
            } else if (Response.class.isAssignableFrom(ctx.getMethod().getReturnType())) {
                return Response.serverError().entity("INTERNAL SERVER ERROR").build();
            }
            
            return null;
        }
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
