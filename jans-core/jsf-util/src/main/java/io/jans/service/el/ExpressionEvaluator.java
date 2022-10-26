/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import java.io.Serializable;
import java.util.Map;

import jakarta.el.ExpressionFactory;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@Dependent
public class ExpressionEvaluator implements Serializable {

    private static final long serialVersionUID = -16629423172996440L;

    @Inject
    private ExtendedELContext ctx;

    @Inject
    private ExpressionFactory expressionFactory;

    public <T> T evaluateValueExpression(String expression, Class<T> expectedType, Map<String, Object> parameters) {
        if ((parameters == null) || (parameters.size() == 0)) {
            return (T) evaluateValueExpression(expression, expectedType);
        }

        ConstantResolver constantResolver = ctx.getConstantResolver();
        try {
            // Setting parameters
            for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
                constantResolver.addConstant(parameter.getKey(), parameter.getValue());
            }

            return (T) expressionFactory.createValueExpression(ctx, expression, expectedType).getValue(ctx);
        } finally {
            // Clearing up parameters
            for (String parameterName : parameters.keySet()) {
                constantResolver.removeConstant(parameterName);
            }
        }
    }

    public <T> T evaluateValueExpression(String expression, Class<T> expectedType) {
        return (T) expressionFactory.createValueExpression(ctx, expression, expectedType).getValue(ctx);
    }

    public Object evaluateValueExpression(String expression) {
        return evaluateValueExpression(expression, Object.class);
    }

    public <T> T invokeMethodExpression(String expression, Class<T> expectedReturnType, Object[] args, Class<?>[] argTypes) {
        return (T) expressionFactory.createMethodExpression(ctx, expression, expectedReturnType, argTypes).invoke(ctx, args);
    }

    public <T> T invokeMethodExpression(String expression, Class<T> expectedReturnType) {
        return invokeMethodExpression(expression, expectedReturnType, new Object[0], new Class[0]);
    }

    public Object invokeMethodExpression(String expression) {
        return invokeMethodExpression(expression, Object.class, new Object[0], new Class[0]);
    }

    public Object invokeMethodExpression(String expression, Object... args) {
        return invokeMethodExpression(expression, Object.class, args, new Class[args.length]);
    }

    public <T> T resolveName(String name, Class<T> expectedType) {
        return evaluateValueExpression(toExpression(name), expectedType);
    }

    public Object resolveName(String name) {
        return resolveName(name, Object.class);
    }

    private String toExpression(String name) {
        return "#{" + name + "}";
    }

}
