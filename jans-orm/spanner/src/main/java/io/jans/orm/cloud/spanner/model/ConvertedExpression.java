/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.model;

import java.util.Map;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.Join;

/**
 * Filter to Expression convertation result
 *
 * @author Yuriy Movchan Date: 12/16/2020
 */
public class ConvertedExpression {
	
	private Expression expression;
	private Map<String, ValueWithStructField> queryParameters;
	private Map<String, Join> joinTables;

	private ConvertedExpression(Expression expression) {
		this.expression = expression;
	}

	private ConvertedExpression(Expression expression, Map<String, ValueWithStructField> queryParameters, Map<String, Join> joinTables) {
		this.expression = expression;
		this.queryParameters = queryParameters;
		this.joinTables = joinTables;
	}

	public static ConvertedExpression build(Expression expression, Map<String, ValueWithStructField> queryParameters, Map<String, Join> joinTables) {
		return new ConvertedExpression(expression, queryParameters, joinTables);
	}

	public Expression expression() {
		return expression;
	}

	public Map<String, ValueWithStructField> queryParameters() {
		return queryParameters;
	}

	public Map<String, Join> joinTables() {
		return joinTables;
	}

	@Override
	public String toString() {
		return "ConvertedExpression [expression=" + expression + ", queryParameters=" + queryParameters
				+ ", joinTables=" + joinTables + "]";
	}

}
