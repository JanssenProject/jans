package io.jans.orm.couchbase.impl.test;

import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.search.filter.Filter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.couchbase.client.java.json.JsonObject;

import io.jans.orm.couchbase.impl.CouchbaseFilterConverter;
import io.jans.orm.couchbase.model.ConvertedExpression;
import io.jans.orm.couchbase.operation.CouchbaseOperationService;
import io.jans.orm.couchbase.operation.impl.CouchbaseOperationServiceImpl;

public class CouchbaseFilterConverterTest {

	private CouchbaseFilterConverter simpleConverter;
	private CouchbaseOperationService couchbaseOperationService;

	@BeforeClass
	public void init() {
		this.couchbaseOperationService = new CouchbaseOperationServiceImpl();
		this.simpleConverter = new CouchbaseFilterConverter(couchbaseOperationService);
	}

	@Test
	public void checkEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		ConvertedExpression expressionEq1 = simpleConverter.convertToCouchbaseFilter(filterEq1, null, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(expressionEq1.expression(), "( ( uid = $uid ) OR ( $uid IN uid ) )");
		assertEquals(queryEq1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( ( uid = \"test\" ) OR ( \"test\" IN uid ) )");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23);
		ConvertedExpression expressionEq2 = simpleConverter.convertToCouchbaseFilter(filterEq2, null, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(expressionEq2.expression(), "( ( age = $age ) OR ( $age IN age ) )");
		assertEquals(queryEq2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( ( age = 23 ) OR ( 23 IN age ) )");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L);
		ConvertedExpression expressionEq3 = simpleConverter.convertToCouchbaseFilter(filterEq3, null, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(expressionEq3.expression(), "( ( age = $age ) OR ( $age IN age ) )");
		assertEquals(queryEq3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( ( age = 23 ) OR ( 23 IN age ) )");

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionEq4 = simpleConverter.convertToCouchbaseFilter(filterEq4, null, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(expressionEq4.expression(), "( ( added = $added ) OR ( $added IN added ) )");
		assertEquals(queryEq4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( ( added = \"2020-12-16T14:58:18.398Z\" ) OR ( \"2020-12-16T14:58:18.398Z\" IN added ) )");
	}

	@Test
	public void checkMultivaluedEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test").multiValued();
		ConvertedExpression expressionEq1 = simpleConverter.convertToCouchbaseFilter(filterEq1, null, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(expressionEq1.expression(), "ANY uid_ IN uid SATISFIES uid_ = $uid END");
		assertEquals(queryEq1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ = \"test\" END");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23).multiValued();
		ConvertedExpression expressionEq2 = simpleConverter.convertToCouchbaseFilter(filterEq2, null, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(expressionEq2.expression(), "ANY age_ IN age SATISFIES age_ = $age END");
		assertEquals(queryEq2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY age_ IN age SATISFIES age_ = 23 END");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L).multiValued();
		ConvertedExpression expressionEq3 = simpleConverter.convertToCouchbaseFilter(filterEq3, null, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(expressionEq3.expression(), "ANY age_ IN age SATISFIES age_ = $age END");
		assertEquals(queryEq3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY age_ IN age SATISFIES age_ = 23 END");

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionEq4 = simpleConverter.convertToCouchbaseFilter(filterEq4, null, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(expressionEq4.expression(), "ANY added_ IN added SATISFIES added_ = $added END");
		assertEquals(queryEq4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY added_ IN added SATISFIES added_ = \"2020-12-16T14:58:18.398Z\" END");
	}

	@Test
	public void checkSinglevaluedEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test").multiValued(false);
		ConvertedExpression expressionEq1 = simpleConverter.convertToCouchbaseFilter(filterEq1, null, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(expressionEq1.expression(), "uid = $uid");
		assertEquals(queryEq1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid = \"test\"");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23).multiValued(false);
		ConvertedExpression expressionEq2 = simpleConverter.convertToCouchbaseFilter(filterEq2, null, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(expressionEq2.expression(), "age = $age");
		assertEquals(queryEq2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE age = 23");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L).multiValued(false);
		ConvertedExpression expressionEq3 = simpleConverter.convertToCouchbaseFilter(filterEq3, null, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(expressionEq3.expression(), "age = $age");
		assertEquals(queryEq3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE age = 23");

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued(false);
		ConvertedExpression expressionEq4 = simpleConverter.convertToCouchbaseFilter(filterEq4, null, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(expressionEq4.expression(), "added = $added");
		assertEquals(queryEq4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE added = \"2020-12-16T14:58:18.398Z\"");
	}

	@Test
	public void checkLeFilters() throws SearchException {
		// LE -- String
		Filter filterLe1 = Filter.createLessOrEqualFilter("uid", "test");
		ConvertedExpression expressionLe1 = simpleConverter.convertToCouchbaseFilter(filterLe1, null, null);

		String queryLe1 = toSelectSQL(expressionLe1);
		assertEquals(expressionLe1.expression(), "uid <= $uid");
		assertEquals(queryLe1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid <= \"test\"");

		// LE -- Integer
		Filter filterLe2 = Filter.createLessOrEqualFilter("age", 23);
		ConvertedExpression expressionLe2 = simpleConverter.convertToCouchbaseFilter(filterLe2, null, null);

		String queryLe2 = toSelectSQL(expressionLe2);
		assertEquals(expressionLe2.expression(), "age <= $age");
		assertEquals(queryLe2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE age <= 23");

		// LE -- Long
		Filter filterLe3 = Filter.createLessOrEqualFilter("age", 23L);
		ConvertedExpression expressionLe3 = simpleConverter.convertToCouchbaseFilter(filterLe3, null, null);

		String queryLe3 = toSelectSQL(expressionLe3);
		assertEquals(expressionLe3.expression(), "age <= $age");
		assertEquals(queryLe3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE age <= 23");

		// LE -- Date
		Filter filterLe4 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionLe4 = simpleConverter.convertToCouchbaseFilter(filterLe4, null, null);

		String queryLe4 = toSelectSQL(expressionLe4);
		assertEquals(expressionLe4.expression(), "added <= $added");
		assertEquals(queryLe4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE added <= \"2020-12-16T14:58:18.398Z\"");
	}

	@Test
	public void checkMultivaluedLeFilters() throws SearchException {
		// LE -- String
		Filter filterLe1 = Filter.createLessOrEqualFilter("uid", "test").multiValued();
		ConvertedExpression expressionLe1 = simpleConverter.convertToCouchbaseFilter(filterLe1, null, null);

		String queryLe1 = toSelectSQL(expressionLe1);
		assertEquals(expressionLe1.expression(), "ANY uid_ IN uid SATISFIES uid_ <= $uid END");
		assertEquals(queryLe1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ <= \"test\" END");

		// LE -- Integer
		Filter filterLe2 = Filter.createLessOrEqualFilter("age", 23).multiValued();
		ConvertedExpression expressionLe2 = simpleConverter.convertToCouchbaseFilter(filterLe2, null, null);

		String queryLe2 = toSelectSQL(expressionLe2);
		assertEquals(expressionLe2.expression(), "ANY age_ IN age SATISFIES age_ <= $age END");
		assertEquals(queryLe2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY age_ IN age SATISFIES age_ <= 23 END");

		// LE -- Long
		Filter filterLe3 = Filter.createLessOrEqualFilter("age", 23L).multiValued();
		ConvertedExpression expressionLe3 = simpleConverter.convertToCouchbaseFilter(filterLe3, null, null);

		String queryLe3 = toSelectSQL(expressionLe3);
		assertEquals(expressionLe3.expression(), "ANY age_ IN age SATISFIES age_ <= $age END");
		assertEquals(queryLe3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY age_ IN age SATISFIES age_ <= 23 END");

		// LE -- Date
		Filter filterLe4 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionLe4 = simpleConverter.convertToCouchbaseFilter(filterLe4, null, null);

		String queryLe4 = toSelectSQL(expressionLe4);
		assertEquals(expressionLe4.expression(), "ANY added_ IN added SATISFIES added_ <= $added END");
		assertEquals(queryLe4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY added_ IN added SATISFIES added_ <= \"2020-12-16T14:58:18.398Z\" END");

		// LE -- Date
		Filter filterLe5 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued(3);
		ConvertedExpression expressionLe5 = simpleConverter.convertToCouchbaseFilter(filterLe5, null, null);

		String queryLe5 = toSelectSQL(expressionLe5);
		assertEquals(expressionLe5.expression(), "ANY added_ IN added SATISFIES added_ <= $added END");
		assertEquals(queryLe5, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY added_ IN added SATISFIES added_ <= \"2020-12-16T14:58:18.398Z\" END");
	}

	@Test
	public void checkGeFilters() throws SearchException {
		// LE -- String
		Filter filterGe1 = Filter.createGreaterOrEqualFilter("uid", "test");
		ConvertedExpression expressionGe1 = simpleConverter.convertToCouchbaseFilter(filterGe1, null, null);

		String queryGe1 = toSelectSQL(expressionGe1);
		assertEquals(expressionGe1.expression(), "uid >= $uid");
		assertEquals(queryGe1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid >= \"test\"");

		// LE -- Integer
		Filter filterGe2 = Filter.createGreaterOrEqualFilter("age", 23);
		ConvertedExpression expressionGe2 = simpleConverter.convertToCouchbaseFilter(filterGe2, null, null);

		String queryGe2 = toSelectSQL(expressionGe2);
		assertEquals(expressionGe2.expression(), "age >= $age");
		assertEquals(queryGe2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE age >= 23");

		// LE -- Long
		Filter filterGe3 = Filter.createGreaterOrEqualFilter("age", 23L);
		ConvertedExpression expressionGe3 = simpleConverter.convertToCouchbaseFilter(filterGe3, null, null);

		String queryGe3 = toSelectSQL(expressionGe3);
		assertEquals(expressionGe3.expression(), "age >= $age");
		assertEquals(queryGe3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE age >= 23");

		// LE -- Date
		Filter filterGe4 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionGe4 = simpleConverter.convertToCouchbaseFilter(filterGe4, null, null);

		String queryGe4 = toSelectSQL(expressionGe4);
		assertEquals(expressionGe4.expression(), "added >= $added");
		assertEquals(queryGe4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE added >= \"2020-12-16T14:58:18.398Z\"");
	}

	@Test
	public void checkMultivaluedGeFilters() throws SearchException {
		// GE -- String
		Filter filterGe1 = Filter.createGreaterOrEqualFilter("uid", "test").multiValued();
		ConvertedExpression expressionGe1 = simpleConverter.convertToCouchbaseFilter(filterGe1, null, null);

		String queryGe1 = toSelectSQL(expressionGe1);
		assertEquals(expressionGe1.expression(), "ANY uid_ IN uid SATISFIES uid_ >= $uid END");
		assertEquals(queryGe1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ >= \"test\" END");

		// GE -- Integer
		Filter filterGe2 = Filter.createGreaterOrEqualFilter("age", 23).multiValued();
		ConvertedExpression expressionGe2 = simpleConverter.convertToCouchbaseFilter(filterGe2, null, null);

		String queryGe2 = toSelectSQL(expressionGe2);
		assertEquals(expressionGe2.expression(), "ANY age_ IN age SATISFIES age_ >= $age END");
		assertEquals(queryGe2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY age_ IN age SATISFIES age_ >= 23 END");

		// GE -- Long
		Filter filterGe3 = Filter.createGreaterOrEqualFilter("age", 23L).multiValued();
		ConvertedExpression expressionGe3 = simpleConverter.convertToCouchbaseFilter(filterGe3, null, null);

		String queryGe3 = toSelectSQL(expressionGe3);
		assertEquals(expressionGe3.expression(), "ANY age_ IN age SATISFIES age_ >= $age END");
		assertEquals(queryGe3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY age_ IN age SATISFIES age_ >= 23 END");

		// GE -- Date
		Filter filterGe4 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionGe4 = simpleConverter.convertToCouchbaseFilter(filterGe4, null, null);

		String queryGe4 = toSelectSQL(expressionGe4);
		assertEquals(expressionGe4.expression(), "ANY added_ IN added SATISFIES added_ >= $added END");
		assertEquals(queryGe4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY added_ IN added SATISFIES added_ >= \"2020-12-16T14:58:18.398Z\" END");

		// GE -- Date
		Filter filterGe5 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued(3);
		ConvertedExpression expressionGe5 = simpleConverter.convertToCouchbaseFilter(filterGe5, null, null);

		String queryGe5 = toSelectSQL(expressionGe5);
		assertEquals(expressionGe5.expression(), "ANY added_ IN added SATISFIES added_ >= $added END");
		assertEquals(queryGe5, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY added_ IN added SATISFIES added_ >= \"2020-12-16T14:58:18.398Z\" END");
	}

	@Test
	public void checkPresenceFilters() throws SearchException {
		// Presence -- String
		Filter filterPresence = Filter.createPresenceFilter("uid");
		ConvertedExpression expressionPresence = simpleConverter.convertToCouchbaseFilter(filterPresence, null, null);

		String queryPresence = toSelectSQL(expressionPresence);
		assertEquals(expressionPresence.expression(), "uid IS NOT MISSING");
		assertEquals(queryPresence, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid IS NOT MISSING");
	}

	@Test
	public void checkMultivaluedPresenceFilters() throws SearchException {
		// Presence -- String
		Filter filterPresence1 = Filter.createPresenceFilter("uid").multiValued();
		ConvertedExpression expressionPresence1 = simpleConverter.convertToCouchbaseFilter(filterPresence1, null, null);

		String queryPresence1 = toSelectSQL(expressionPresence1);
		assertEquals(expressionPresence1.expression(), "ANY uid_ IN uid SATISFIES uid_ IS NOT MISSING END");
		assertEquals(queryPresence1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ IS NOT MISSING END");

		// Presence -- String -- Multivalued = 3
		Filter filterPresence2 = Filter.createPresenceFilter("uid").multiValued(3);
		ConvertedExpression expressionPresence2 = simpleConverter.convertToCouchbaseFilter(filterPresence2, null, null);

		String queryPresence2 = toSelectSQL(expressionPresence2);
		assertEquals(expressionPresence2.expression(), "ANY uid_ IN uid SATISFIES uid_ IS NOT MISSING END");
		assertEquals(queryPresence2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ IS NOT MISSING END");
	}

	@Test
	public void checkSubFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, null);
		ConvertedExpression expressionSub1 = simpleConverter.convertToCouchbaseFilter(filterSub1, null, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(expressionSub1.expression(), "uid LIKE %$uid_any%");
		assertEquals(querySub1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid LIKE \"%test%\"");

		Filter filterSub2 = Filter.createSubstringFilter("uid", "a", new String[] { "test" }, null);
		ConvertedExpression expressionSub2 = simpleConverter.convertToCouchbaseFilter(filterSub2, null, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(expressionSub2.expression(), "uid LIKE $uid_i%$uid_any%");
		assertEquals(querySub2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid LIKE \"a%test%\"");

		Filter filterSub3 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z");
		ConvertedExpression expressionSub3 = simpleConverter.convertToCouchbaseFilter(filterSub3, null, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(expressionSub3.expression(), "uid LIKE %$uid_any%$uid_f");
		assertEquals(querySub3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid LIKE \"%test%z\"");
	}

	@Test
	public void checkMultivaluedSubFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub1 = simpleConverter.convertToCouchbaseFilter(filterSub1, null, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(expressionSub1.expression(), "ANY uid_ IN uid SATISFIES uid_ LIKE %$uid_any% END");
		assertEquals(querySub1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ LIKE \"%test%\" END");

		Filter filterSub2 = Filter.createSubstringFilter("uid", "a", new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub2 = simpleConverter.convertToCouchbaseFilter(filterSub2, null, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(expressionSub2.expression(), "ANY uid_ IN uid SATISFIES uid_ LIKE $uid_i%$uid_any% END");
		assertEquals(querySub2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ LIKE \"a%test%\" END");

		Filter filterSub3 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z").multiValued();
		ConvertedExpression expressionSub3 = simpleConverter.convertToCouchbaseFilter(filterSub3, null, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(expressionSub3.expression(), "ANY uid_ IN uid SATISFIES uid_ LIKE %$uid_any%$uid_f END");
		assertEquals(querySub3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ LIKE \"%test%z\" END");

		Filter filterSub4 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z").multiValued(3);
		ConvertedExpression expressionSub4 = simpleConverter.convertToCouchbaseFilter(filterSub4, null, null);

		String querySub4 = toSelectSQL(expressionSub4);
		assertEquals(expressionSub4.expression(), "ANY uid_ IN uid SATISFIES uid_ LIKE %$uid_any%$uid_f END");
		assertEquals(querySub4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES uid_ LIKE \"%test%z\" END");
	}

	@Test
	public void checkMultivaluedSubWithLowerFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub1 = simpleConverter.convertToCouchbaseFilter(filterSub1, null, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(expressionSub1.expression(), "ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE %$uid_any% END");
		assertEquals(querySub1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE \"%test%\" END");

		Filter filterSub2 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), "a", new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub2 = simpleConverter.convertToCouchbaseFilter(filterSub2, null, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(expressionSub1.expression(), "ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE %$uid_any% END");
		assertEquals(querySub2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE \"a%test%\" END");

		Filter filterSub3 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, new String[] { "test" }, "z").multiValued();
		ConvertedExpression expressionSub3 = simpleConverter.convertToCouchbaseFilter(filterSub3, null, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(expressionSub1.expression(), "ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE %$uid_any% END");
		assertEquals(querySub3, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE \"%test%z\" END");

		Filter filterSub4 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, new String[] { "test" }, "z").multiValued(3);
		ConvertedExpression expressionSub4 = simpleConverter.convertToCouchbaseFilter(filterSub4, null, null);

		String querySub4 = toSelectSQL(expressionSub4);
		assertEquals(expressionSub1.expression(), "ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE %$uid_any% END");
		assertEquals(querySub4, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES LOWER( uid_ ) LIKE \"%test%z\" END");
	}

	@Test
	public void checkLowerFilters() throws SearchException {
		Filter userUidFilter1 = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test");

		ConvertedExpression expressionUserUid1 = simpleConverter.convertToCouchbaseFilter(userUidFilter1, null, null);

		String queryUserUid1 = toSelectSQL(expressionUserUid1);
		assertEquals(expressionUserUid1.expression(), "LOWER( uid ) = $uid");
		assertEquals(queryUserUid1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE LOWER( uid ) = \"test\"");
	}

	@Test
	public void checkMultivaluedLowerFilters() throws SearchException {
		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test").multiValued();

		ConvertedExpression expressionUserUid = simpleConverter.convertToCouchbaseFilter(userUidFilter, null, null);

		String queryUserUid = toSelectSQL(expressionUserUid);
		assertEquals(expressionUserUid.expression(), "ANY uid_ IN uid SATISFIES LOWER( uid_ ) = $uid END");
		assertEquals(queryUserUid, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ANY uid_ IN uid SATISFIES LOWER( uid_ ) = \"test\" END");
	}

	@Test
	public void checkSinglevaluedLowerFilters() throws SearchException {
		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test").multiValued(false);

		ConvertedExpression expressionUserUid = simpleConverter.convertToCouchbaseFilter(userUidFilter, null, null);

		String queryUserUid = toSelectSQL(expressionUserUid);
		assertEquals(expressionUserUid.expression(), "LOWER( uid ) = $uid");
		assertEquals(queryUserUid, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE LOWER( uid ) = \"test\"");
	}

	@Test
	public void checkNotFilters() throws SearchException {
		Filter notFilter1 = Filter.createNOTFilter(Filter.createLessOrEqualFilter("age", 23));

		ConvertedExpression expressionNot1 = simpleConverter.convertToCouchbaseFilter(notFilter1, null, null);

		String queryUserUid1 = toSelectSQL(expressionNot1);
		assertEquals(expressionNot1.expression(), "NOT ( age <= $age )");
		assertEquals(queryUserUid1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE NOT ( age <= 23 )");

		Filter notFilter2 = Filter.createNOTFilter(Filter.createANDFilter(Filter.createLessOrEqualFilter("age", 23), Filter.createGreaterOrEqualFilter("age", 25)));

		ConvertedExpression expressionNot2 = simpleConverter.convertToCouchbaseFilter(notFilter2, null, null);

		String queryUserUid2 = toSelectSQL(expressionNot2);
		assertEquals(expressionNot2.expression(), "NOT ( ( age <= $age AND age >= $_age_0 ) )");
		assertEquals(queryUserUid2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE NOT ( ( age <= 23 AND age >= 25 ) )");
	}

	@Test
	public void checkAndFilters() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterPresence1 = Filter.createPresenceFilter("mail");
		Filter filterLe1 = Filter.createLessOrEqualFilter("age", 23);
		Filter filterAnd1 = Filter.createANDFilter(filterPresence1, filterEq1, filterLe1);
		ConvertedExpression expressionAnd1 = simpleConverter.convertToCouchbaseFilter(filterAnd1, null, null);

		String queryAnd1 = toSelectSQL(expressionAnd1);
		assertEquals(expressionAnd1.expression(), "( mail IS NOT MISSING AND ( ( uid = $uid ) OR ( $uid IN uid ) ) AND age <= $age )");
		assertEquals(queryAnd1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( mail IS NOT MISSING AND ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) AND age <= 23 )");
	}

	@Test
	public void checkOrFilters() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterPresence1 = Filter.createPresenceFilter("mail");
		Filter filterLe1 = Filter.createLessOrEqualFilter("age", 23);
		Filter filterOr1 = Filter.createORFilter(filterPresence1, filterEq1, filterLe1);
		ConvertedExpression expressionAnd1 = simpleConverter.convertToCouchbaseFilter(filterOr1, null, null);

		String queryAnd1 = toSelectSQL(expressionAnd1);
		assertEquals(expressionAnd1.expression(), "( mail IS NOT MISSING OR ( ( uid = $uid ) OR ( $uid IN uid ) ) OR age <= $age )");
		assertEquals(queryAnd1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( mail IS NOT MISSING OR ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) OR age <= 23 )");
	}

	@Test
	public void checkOrJoinFilters() throws SearchException {
		// And with join
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterEq2 = Filter.createEqualityFilter("uid", "test2");
		Filter filterEq3 = Filter.createEqualityFilter("uid", "test3");
		Filter filterOr1 = Filter.createORFilter(filterEq1, filterEq2, filterEq3).multiValued(false);
		ConvertedExpression expressionOr1 = simpleConverter.convertToCouchbaseFilter(filterOr1, null, null);

		String queryOr1 = toSelectSQL(expressionOr1);
		assertEquals(expressionOr1.expression(), "uid IN [ $uid, $_uid_0, $_uid_1 ]");
		assertEquals(queryOr1, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE uid IN [ \"test\", \"test2\", \"test3\" ]");

		Filter filterOr2 = Filter.createORFilter(filterEq1, filterEq2, filterEq3);
		ConvertedExpression expressionOr2 = simpleConverter.convertToCouchbaseFilter(filterOr2, null, null);

		String queryOr2 = toSelectSQL(expressionOr2);
		assertEquals(expressionOr2.expression(), "( ( ( uid = $uid ) OR ( $uid IN uid ) ) OR ( ( uid = $_uid_0 ) OR ( $_uid_0 IN uid ) ) OR ( ( uid = $_uid_1 ) OR ( $_uid_1 IN uid ) ) )");
		assertEquals(queryOr2, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( ( ( uid = \"test\" ) OR ( \"test\" IN uid ) ) OR ( ( uid = \"test2\" ) OR ( \"test2\" IN uid ) ) OR ( ( uid = \"test3\" ) OR ( \"test3\" IN uid ) ) )");
	}

	@Test
	public void checkOrWithLowerCaseFilter() throws SearchException {
        boolean useLowercaseFilter = true;

        String[] targetArray = new String[] { "test_value" };

        Filter descriptionFilter, displayNameFilter;
		if (useLowercaseFilter) {
			descriptionFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("description"), null, targetArray, null);
			displayNameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter("displayName"), null, targetArray, null);
		} else {
			descriptionFilter = Filter.createSubstringFilter("description", null, targetArray, null);
			displayNameFilter = Filter.createSubstringFilter("displayName", null, targetArray, null);
		}

		Filter searchFilter = Filter.createORFilter(descriptionFilter, displayNameFilter);
		Filter typeFilter = Filter.createEqualityFilter("jansScrTyp", "person_authentication");
        Filter filter = Filter.createANDFilter(searchFilter, typeFilter);
        
		ConvertedExpression expression = simpleConverter.convertToCouchbaseFilter(filter, null, null);
		String query = toSelectSQL(expression);
		assertEquals(expression.expression(), "( ( LOWER( description ) LIKE %$description_any% OR LOWER( displayName ) LIKE %$displayName_any% ) AND ( ( jansScrTyp = $jansScrTyp ) OR ( $jansScrTyp IN jansScrTyp ) ) )");
		assertEquals(query, "SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE ( ( LOWER( description ) LIKE \"%test_value%\" OR LOWER( displayName ) LIKE \"%test_value%\" ) AND ( ( jansScrTyp = \"person_authentication\" ) OR ( \"person_authentication\" IN jansScrTyp ) ) )");
	}

	private String toSelectSQL(ConvertedExpression convertedExpression) {
		String select = String.format("SELECT jans_doc.* FROM `gluu` AS jans_doc WHERE %s", convertedExpression.expression());
		
		// Substitute parameters for test
		JsonObject params = convertedExpression.getQueryParameters();
		for (String name : params.getNames()) {
			Object value = params.get(name);

			Object replaceValue = value;
			if (value instanceof String) {
				replaceValue = "\"" + value + "\"";
			}
			
			String searchName = "\\$" + name;
			int subIndex = select.indexOf("%$" + name + "%");
			if (subIndex != -1) {
				searchName = "%" + searchName + "%";
				replaceValue = "\"%" + value + "%\"";
			}
			select = select.replaceAll(searchName, replaceValue.toString());
		}
		
		select = select.replaceAll("\"\"%", "%").replaceAll("%\"\"", "%");

		return select;
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
