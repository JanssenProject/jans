/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.util.Map;

import com.querydsl.core.types.Expression;

/**
 * Filter to Expression convertation result
 *
 * @author Yuriy Movchan Date: 12/16/2020
 */
public class ConvertedExpression {
	
	private Expression expression;
	private Map<String, Class<?>> jsonAttributes;

	private ConvertedExpression(Expression expression) {
		this.expression = expression;
	}

	private ConvertedExpression(Expression expression, Map<String, Class<?>> jsonAttributes) {
		this.expression = expression;
		this.jsonAttributes = jsonAttributes;
	}

	public static ConvertedExpression build(Expression expression, Map<String, Class<?>> jsonAttributes) {
		return new ConvertedExpression(expression, jsonAttributes);
	}

	public Expression expression() {
		return expression;
	}

	public Map<String, Class<?>> jsonAttributes() {
		return jsonAttributes;
	}

	@Override
	public String toString() {
		return "ConvertedExpression [expression=" + expression + ", jsonAttributes=" + jsonAttributes + "]";
	}

}
