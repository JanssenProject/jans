/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxtrust.auth.uma.annotations;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.gluu.service.el.ExpressionEvaluator;
import org.gluu.service.security.InterceptSecure;
import org.gluu.service.security.Secure;
import org.gluu.service.security.SecureVariable;
import org.gluu.service.security.SecurityEvaluationException;
import org.gluu.service.security.SecurityExtension;
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
