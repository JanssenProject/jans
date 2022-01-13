package io.jans.orm.couchbase.impl.test;

import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.path.GroupByPath;

import io.jans.orm.couchbase.impl.CouchbaseFilterConverter;
import io.jans.orm.couchbase.model.ConvertedExpression;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterProcessor;

public class CouchbaseFilterConverterCheckExcludeFilterTest {

	private CouchbaseFilterConverter simpleConverter;
	private FilterProcessor filterProcessor;

	@BeforeClass
	public void init() {
		this.simpleConverter = new CouchbaseFilterConverter(null);
		this.filterProcessor = new FilterProcessor();
	}

	@Test
	public void checkObjectClassExcludeFilter() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterEq2 = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test");
		Filter filterEq3 = Filter.createEqualityFilter("objectClass", "jansPerson");
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();

		Filter andFilter = Filter.createANDFilter(filterEq1, filterEq2, filterEq3, filterEq4);
		Filter orFilter = Filter.createANDFilter(filterEq1, filterEq2, filterEq3, andFilter, filterEq4);
		
		Filter filter1 = Filter.createANDFilter(filterEq3, orFilter);
		ConvertedExpression expression1 = simpleConverter.convertToCouchbaseFilter(filter1, null, null);

		String query1 = toSelectSQL(expression1);
		assertEquals(query1, "SELECT jans_doc.* FROM `jans` AS jans_doc WHERE ( ( ( objectClass = \"jansPerson\" ) OR ( \"jansPerson\" IN objectClass ) ) AND ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND LOWER(uid) = \"test\" AND ( ( objectClass = \"jansPerson\" ) OR ( \"jansPerson\" IN objectClass ) ) AND ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND LOWER(uid) = \"test\" AND ( ( objectClass = \"jansPerson\" ) OR ( \"jansPerson\" IN objectClass ) ) AND ANY added_ IN added SATISFIES added_ = \"Wed Dec 16 14:58:18 UTC 2020\" END ) AND ANY added_ IN added SATISFIES added_ = \"Wed Dec 16 14:58:18 UTC 2020\" END ) )");

		Filter filter2 = filterProcessor.excludeFilter(filter1, filterEq3);

		ConvertedExpression expression2 = simpleConverter.convertToCouchbaseFilter(filter2, null, null);

		String query2 = toSelectSQL(expression2);
		assertEquals(query2, "SELECT jans_doc.* FROM `jans` AS jans_doc WHERE ( ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND LOWER(uid) = \"test\" AND ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND LOWER(uid) = \"test\" AND ANY added_ IN added SATISFIES added_ = \"Wed Dec 16 14:58:18 UTC 2020\" END ) AND ANY added_ IN added SATISFIES added_ = \"Wed Dec 16 14:58:18 UTC 2020\" END ) )");

		Filter filter3 = filterProcessor.excludeFilter(filter1, Filter.createEqualityFilter("objectClass", null));

		ConvertedExpression expression3 = simpleConverter.convertToCouchbaseFilter(filter3, null, null);

		String query3 = toSelectSQL(expression3);
		assertEquals(query3, "SELECT jans_doc.* FROM `jans` AS jans_doc WHERE ( ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND LOWER(uid) = \"test\" AND ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND LOWER(uid) = \"test\" AND ANY added_ IN added SATISFIES added_ = \"Wed Dec 16 14:58:18 UTC 2020\" END ) AND ANY added_ IN added SATISFIES added_ = \"Wed Dec 16 14:58:18 UTC 2020\" END ) )");
	}

	private String toSelectSQL(ConvertedExpression convertedExpression) {
		GroupByPath select = Select.select("jans_doc.*").from(Expression.i("jans")).as("jans_doc").where(convertedExpression.expression());

		return select.toString();
	}

	private static Date getUtcDateFromMillis(long millis) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(millis);
		calendar.set(Calendar.ZONE_OFFSET, TimeZone.getTimeZone("UTC").getRawOffset());

		Date date = calendar.getTime();

		return date;
	}

}
