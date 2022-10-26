/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.model;

import java.util.HashSet;
import java.util.Set;

import com.couchbase.client.java.json.JsonObject;

/**
 * Filter to N1QL transformation result
 *
 * @author Yuriy Movchan Date: 06/21/2019
 */
public class ConvertedExpression {
	
	private String expression;
	private JsonObject queryParameters;
	private boolean consistency;
	private Set<String> singleLevelParameters;

	private ConvertedExpression(String expression, JsonObject queryParameters) {
		this.expression = expression;
		this.queryParameters = queryParameters;
		this.singleLevelParameters = new HashSet<String>();
	}

	private ConvertedExpression(String expression, JsonObject queryParameters, boolean consistency) {
		this(expression, queryParameters);
		this.consistency = consistency;
	}

	public static ConvertedExpression build(String expression, JsonObject queryParameters, boolean consistency) {
		return new ConvertedExpression(expression, queryParameters, consistency);
	}

	public static ConvertedExpression build(String expression, JsonObject queryParameters, String queryParameter, boolean consistency) {
		ConvertedExpression convertedExpression = new ConvertedExpression(expression, queryParameters, consistency);
		convertedExpression.getSingleLevelParameters().add(queryParameter);
		
		return convertedExpression;
	}

	public String expression() {
		return expression;
	}

	public JsonObject getQueryParameters() {
		return queryParameters;
	}

	public boolean consistency() {
		return consistency;
	}

	public void consistency(boolean consistency) {
		this.consistency = consistency;
	}

	public Set<String> getSingleLevelParameters() {
		return singleLevelParameters;
	}

	@Override
	public String toString() {
		return "ConvertedExpression [expression=" + expression + ", queryParameters=" + queryParameters
				+ ", consistency=" + consistency + ", singleLevelParameters=" + singleLevelParameters + "]";
	}

}
