package org.xdi.service.security;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.xdi.service.el.ExpressionEvaluator;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@Interceptor
@InterceptSecure({})
public class SecurityInterceptor implements Serializable {

	private static final String KEY = "org.jboss.weld.interceptor.bindings";
	
	@Inject
	private SecurityExtension securityExtension;
    
    @Inject
    private ExpressionEvaluator expressionEvaluator;

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
    	InterceptSecure is = securityExtension.getInterceptSecure(ctx.getMethod());

        // SecurityChecking  restrictions
        Secure[] constraints = (is == null) ? new Secure[0] : is.value();

        // Getting the parameter values
        Map<String, Object> secureVars = computeParameterValues(ctx);

        for (Secure constraint : constraints) {
            Boolean expressionValue = expressionEvaluator.evaluateValueExpression(constraint.value(), Boolean.class, secureVars);

            if ((expressionValue == null) || !expressionValue) {
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
