package org.gluu.persist.couchbase.model;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * Filter to Expression convertation result
 *
 * @author Yuriy Movchan Date: 06/21/2019
 */
public class ConvertedExpression {
	
	private Expression expression;
	private boolean consistency;

	private ConvertedExpression(Expression expression) {
		this.expression = expression;
	}
	
	public static ConvertedExpression build(Expression expression, boolean consistency) {
		return new ConvertedExpression(expression);
	}

	public Expression expression() {
		return expression;
	}

	public boolean consistency() {
		return consistency;
	}

	public void consistency(boolean consistency) {
		this.consistency = consistency;
	}

}
