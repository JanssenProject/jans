package org.xdi.service.el;

import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
public class ExpressionEvaluator implements Serializable {

	private static final long serialVersionUID = -16629423172996440L;

	@Inject
	private Instance<ELContext> elContext;

	@Inject
	private ExpressionFactory expressionFactory;

	public ELContext getELContext() {
		return elContext.get();
	}

	public <T> T evaluateValueExpression(String expression, Class<T> expectedType) {
		ELContext ctx = elContext.get();
		return (T) expressionFactory.createValueExpression(ctx, expression, expectedType).getValue(ctx);
	}

	public Object evaluateValueExpression(String expression) {
		return evaluateValueExpression(expression, Object.class);
	}

	public <T> T invokeMethodExpression(String expression, Class<T> expectedReturnType, Object[] args, Class<?>[] argTypes) {
		ELContext ctx = elContext.get();
		return (T) expressionFactory.createMethodExpression(ctx, expression, expectedReturnType, argTypes).invoke(ctx,
				args);
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