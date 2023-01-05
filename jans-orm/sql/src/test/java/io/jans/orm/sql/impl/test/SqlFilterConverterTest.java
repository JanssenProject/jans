package io.jans.orm.sql.impl.test;

import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;

import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.dsl.template.MySQLJsonTemplates;
import io.jans.orm.sql.impl.SqlFilterConverter;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.operation.impl.SqlOperationServiceImpl;

@SuppressWarnings({ "rawtypes", "unchecked"})
public class SqlFilterConverterTest {

	private SqlFilterConverter simpleConverter;
	private Path<Object> tablePath;
	private Path<Object> docAlias;
	private SimpleExpression<Object> tableAlieasPath;
	private StringPath allPath;
	private SQLTemplates sqlTemplates;
	private Configuration configuration;

	@BeforeClass
	public void init() {
		this.simpleConverter = new SqlFilterConverter(new SqlOperationServiceImpl(null, new SqlConnectionProvider(null)));
		this.tablePath = ExpressionUtils.path(Object.class, "table");
		this.docAlias = ExpressionUtils.path(Object.class, "doc");
		this.tableAlieasPath = Expressions.as(tablePath, docAlias);
		this.allPath = Expressions.stringPath(docAlias, "*");

//		this.sqlTemplates = MySQLTemplates.builder().printSchema().build();
		this.sqlTemplates = MySQLJsonTemplates.builder().printSchema().build();
		this.configuration = new Configuration(sqlTemplates);
	}

	@Test
	public void checkEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		ConvertedExpression expressionEq1 = simpleConverter.convertToSqlFilter(null, filterEq1, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(queryEq1, "select doc.`*` from `table` as doc where doc.uid = 'test'");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23);
		ConvertedExpression expressionEq2 = simpleConverter.convertToSqlFilter(null, filterEq2, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(queryEq2, "select doc.`*` from `table` as doc where doc.age = 23");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L);
		ConvertedExpression expressionEq3 = simpleConverter.convertToSqlFilter(null, filterEq3, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(queryEq3, "select doc.`*` from `table` as doc where doc.age = 23");

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionEq4 = simpleConverter.convertToSqlFilter(null, filterEq4, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(queryEq4, "select doc.`*` from `table` as doc where doc.added = (timestamp '2020-12-16 17:58:18')");
	}

	@Test
	public void checkMultivaluedEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test").multiValued();
		ConvertedExpression expressionEq1 = simpleConverter.convertToSqlFilter(null, filterEq1, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(queryEq1, "select doc.`*` from `table` as doc where JSON_CONTAINS(doc.uid->'$.v', CAST('[\"test\"]' AS JSON))");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23).multiValued();
		ConvertedExpression expressionEq2 = simpleConverter.convertToSqlFilter(null, filterEq2, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(queryEq2, "select doc.`*` from `table` as doc where JSON_CONTAINS(doc.age->'$.v', CAST('[23]' AS JSON))");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L).multiValued();
		ConvertedExpression expressionEq3 = simpleConverter.convertToSqlFilter(null, filterEq3, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(queryEq3, "select doc.`*` from `table` as doc where JSON_CONTAINS(doc.age->'$.v', CAST('[23]' AS JSON))");

		

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionEq4 = simpleConverter.convertToSqlFilter(null, filterEq4, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(queryEq4, "select doc.`*` from `table` as doc where JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON))");
	}

	@Test
	public void checkLeFilters() throws SearchException {
		// LE -- String
		Filter filterLe1 = Filter.createLessOrEqualFilter("uid", "test");
		ConvertedExpression expressionLe1 = simpleConverter.convertToSqlFilter(null, filterLe1, null);

		String queryLe1 = toSelectSQL(expressionLe1);
		assertEquals(queryLe1, "select doc.`*` from `table` as doc where doc.uid <= 'test'");

		// LE -- Integer
		Filter filterLe2 = Filter.createLessOrEqualFilter("age", 23);
		ConvertedExpression expressionLe2 = simpleConverter.convertToSqlFilter(null, filterLe2, null);

		String queryLe2 = toSelectSQL(expressionLe2);
		assertEquals(queryLe2, "select doc.`*` from `table` as doc where doc.age <= 23");

		// LE -- Long
		Filter filterLe3 = Filter.createLessOrEqualFilter("age", 23L);
		ConvertedExpression expressionLe3 = simpleConverter.convertToSqlFilter(null, filterLe3, null);

		String queryLe3 = toSelectSQL(expressionLe3);
		assertEquals(queryLe3, "select doc.`*` from `table` as doc where doc.age <= 23");

		// LE -- Date
		Filter filterLe4 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionLe4 = simpleConverter.convertToSqlFilter(null, filterLe4, null);

		String queryLe4 = toSelectSQL(expressionLe4);
		assertEquals(queryLe4, "select doc.`*` from `table` as doc where doc.added <= (timestamp '2020-12-16 17:58:18')");
	}

	@Test
	public void checkMultivaluedLeFilters() throws SearchException {
		// LE -- String
		Filter filterLe1 = Filter.createLessOrEqualFilter("uid", "test").multiValued();
		ConvertedExpression expressionLe1 = simpleConverter.convertToSqlFilter(null, filterLe1, null);

		String queryLe1 = toSelectSQL(expressionLe1);
		assertEquals(queryLe1, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' <= 'test'");

		// LE -- Integer
		Filter filterLe2 = Filter.createLessOrEqualFilter("age", 23).multiValued();
		ConvertedExpression expressionLe2 = simpleConverter.convertToSqlFilter(null, filterLe2, null);

		String queryLe2 = toSelectSQL(expressionLe2);
		assertEquals(queryLe2, "select doc.`*` from `table` as doc where doc.age->'$.v[0]' <= 23");

		// LE -- Long
		Filter filterLe3 = Filter.createLessOrEqualFilter("age", 23L).multiValued();
		ConvertedExpression expressionLe3 = simpleConverter.convertToSqlFilter(null, filterLe3, null);

		String queryLe3 = toSelectSQL(expressionLe3);
		assertEquals(queryLe3, "select doc.`*` from `table` as doc where doc.age->'$.v[0]' <= 23");

		// LE -- Date
		Filter filterLe4 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionLe4 = simpleConverter.convertToSqlFilter(null, filterLe4, null);

		String queryLe4 = toSelectSQL(expressionLe4);
		assertEquals(queryLe4, "select doc.`*` from `table` as doc where doc.added->'$.v[0]' <= (timestamp '2020-12-16 17:58:18')");

		// LE -- Date
		Filter filterLe5 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued(3);
		ConvertedExpression expressionLe5 = simpleConverter.convertToSqlFilter(null, filterLe5, null);

		String queryLe5 = toSelectSQL(expressionLe5);
		assertEquals(queryLe5, "select doc.`*` from `table` as doc where doc.added->'$.v[0]' <= (timestamp '2020-12-16 17:58:18') or doc.added->'$.v[1]' <= (timestamp '2020-12-16 17:58:18') or doc.added->'$.v[2]' <= (timestamp '2020-12-16 17:58:18')");
	}

	@Test
	public void checkGeFilters() throws SearchException {
		// LE -- String
		Filter filterGe1 = Filter.createGreaterOrEqualFilter("uid", "test");
		ConvertedExpression expressionGe1 = simpleConverter.convertToSqlFilter(null, filterGe1, null);

		String queryGe1 = toSelectSQL(expressionGe1);
		assertEquals(queryGe1, "select doc.`*` from `table` as doc where doc.uid >= 'test'");

		// LE -- Integer
		Filter filterGe2 = Filter.createGreaterOrEqualFilter("age", 23);
		ConvertedExpression expressionGe2 = simpleConverter.convertToSqlFilter(null, filterGe2, null);

		String queryGe2 = toSelectSQL(expressionGe2);
		assertEquals(queryGe2, "select doc.`*` from `table` as doc where doc.age >= 23");

		// LE -- Long
		Filter filterGe3 = Filter.createGreaterOrEqualFilter("age", 23L);
		ConvertedExpression expressionGe3 = simpleConverter.convertToSqlFilter(null, filterGe3, null);

		String queryGe3 = toSelectSQL(expressionGe3);
		assertEquals(queryGe3, "select doc.`*` from `table` as doc where doc.age >= 23");

		// LE -- Date
		Filter filterGe4 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionGe4 = simpleConverter.convertToSqlFilter(null, filterGe4, null);

		String queryGe4 = toSelectSQL(expressionGe4);
		assertEquals(queryGe4, "select doc.`*` from `table` as doc where doc.added >= (timestamp '2020-12-16 17:58:18')");
	}

	@Test
	public void checkMultivaluedGeFilters() throws SearchException {
		// GE -- String
		Filter filterGe1 = Filter.createGreaterOrEqualFilter("uid", "test").multiValued();
		ConvertedExpression expressionGe1 = simpleConverter.convertToSqlFilter(null, filterGe1, null);

		String queryGe1 = toSelectSQL(expressionGe1);
		assertEquals(queryGe1, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' >= 'test'");

		// GE -- Integer
		Filter filterGe2 = Filter.createGreaterOrEqualFilter("age", 23).multiValued();
		ConvertedExpression expressionGe2 = simpleConverter.convertToSqlFilter(null, filterGe2, null);

		String queryGe2 = toSelectSQL(expressionGe2);
		assertEquals(queryGe2, "select doc.`*` from `table` as doc where doc.age->'$.v[0]' >= 23");

		// GE -- Long
		Filter filterGe3 = Filter.createGreaterOrEqualFilter("age", 23L).multiValued();
		ConvertedExpression expressionGe3 = simpleConverter.convertToSqlFilter(null, filterGe3, null);

		String queryGe3 = toSelectSQL(expressionGe3);
		assertEquals(queryGe3, "select doc.`*` from `table` as doc where doc.age->'$.v[0]' >= 23");

		// GE -- Date
		Filter filterGe4 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionGe4 = simpleConverter.convertToSqlFilter(null, filterGe4, null);

		String queryGe4 = toSelectSQL(expressionGe4);
		assertEquals(queryGe4, "select doc.`*` from `table` as doc where doc.added->'$.v[0]' >= (timestamp '2020-12-16 17:58:18')");

		// GE -- Date
		Filter filterGe5 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued(3);
		ConvertedExpression expressionGe5 = simpleConverter.convertToSqlFilter(null, filterGe5, null);

		String queryGe5 = toSelectSQL(expressionGe5);
		assertEquals(queryGe5, "select doc.`*` from `table` as doc where doc.added->'$.v[0]' >= (timestamp '2020-12-16 17:58:18') or doc.added->'$.v[1]' >= (timestamp '2020-12-16 17:58:18') or doc.added->'$.v[2]' >= (timestamp '2020-12-16 17:58:18')");
	}

	@Test
	public void checkPresenceFilters() throws SearchException {
		// Presence -- String
		Filter filterPresence = Filter.createPresenceFilter("uid");
		ConvertedExpression expressionPresence = simpleConverter.convertToSqlFilter(null, filterPresence, null);

		String queryPresence = toSelectSQL(expressionPresence);
		assertEquals(queryPresence, "select doc.`*` from `table` as doc where doc.uid is not null");
	}

	@Test
	public void checkMultivaluedPresenceFilters() throws SearchException {
		// Presence -- String
		Filter filterPresence1 = Filter.createPresenceFilter("uid").multiValued();
		ConvertedExpression expressionPresence1 = simpleConverter.convertToSqlFilter(null, filterPresence1, null);

		String queryPresence1 = toSelectSQL(expressionPresence1);
		assertEquals(queryPresence1, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' is not null");

		// Presence -- String -- Multivalued = 3
		Filter filterPresence2 = Filter.createPresenceFilter("uid").multiValued(3);
		ConvertedExpression expressionPresence2 = simpleConverter.convertToSqlFilter(null, filterPresence2, null);

		String queryPresence2 = toSelectSQL(expressionPresence2);
		assertEquals(queryPresence2, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' is not null or doc.uid->'$.v[1]' is not null or doc.uid->'$.v[2]' is not null");
	}

	@Test
	public void checkSubFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, null);
		ConvertedExpression expressionSub1 = simpleConverter.convertToSqlFilter(null, filterSub1, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(querySub1, "select doc.`*` from `table` as doc where doc.uid like '%test%'");

		Filter filterSub2 = Filter.createSubstringFilter("uid", "a", new String[] { "test" }, null);
		ConvertedExpression expressionSub2 = simpleConverter.convertToSqlFilter(null, filterSub2, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(querySub2, "select doc.`*` from `table` as doc where doc.uid like 'a%test%'");

		Filter filterSub3 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z");
		ConvertedExpression expressionSub3 = simpleConverter.convertToSqlFilter(null, filterSub3, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(querySub3, "select doc.`*` from `table` as doc where doc.uid like '%test%z'");
	}

	@Test
	public void checkMultivaluedSubFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub1 = simpleConverter.convertToSqlFilter(null, filterSub1, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(querySub1, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' like '%test%'");

		Filter filterSub2 = Filter.createSubstringFilter("uid", "a", new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub2 = simpleConverter.convertToSqlFilter(null, filterSub2, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(querySub2, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' like 'a%test%'");

		Filter filterSub3 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z").multiValued();
		ConvertedExpression expressionSub3 = simpleConverter.convertToSqlFilter(null, filterSub3, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(querySub3, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' like '%test%z'");

		Filter filterSub4 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z").multiValued(3);
		ConvertedExpression expressionSub4 = simpleConverter.convertToSqlFilter(null, filterSub4, null);

		String querySub4 = toSelectSQL(expressionSub4);
		assertEquals(querySub4, "select doc.`*` from `table` as doc where doc.uid->'$.v[0]' like '%test%z' or doc.uid->'$.v[1]' like '%test%z' or doc.uid->'$.v[2]' like '%test%z'");
	}

	@Test
	public void checkMultivaluedSubWithLowerFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub1 = simpleConverter.convertToSqlFilter(null, filterSub1, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(querySub1, "select doc.`*` from `table` as doc where lower(doc.uid)->'$.v[0]' like '%test%'");

		Filter filterSub2 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), "a", new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub2 = simpleConverter.convertToSqlFilter(null, filterSub2, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(querySub2, "select doc.`*` from `table` as doc where lower(doc.uid)->'$.v[0]' like 'a%test%'");

		Filter filterSub3 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, new String[] { "test" }, "z").multiValued();
		ConvertedExpression expressionSub3 = simpleConverter.convertToSqlFilter(null, filterSub3, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(querySub3, "select doc.`*` from `table` as doc where lower(doc.uid)->'$.v[0]' like '%test%z'");

		Filter filterSub4 = Filter.createSubstringFilter(Filter.createLowercaseFilter("uid"), null, new String[] { "test" }, "z").multiValued(3);
		ConvertedExpression expressionSub4 = simpleConverter.convertToSqlFilter(null, filterSub4, null);

		String querySub4 = toSelectSQL(expressionSub4);
		assertEquals(querySub4, "select doc.`*` from `table` as doc where lower(doc.uid)->'$.v[0]' like '%test%z' or lower(doc.uid)->'$.v[1]' like '%test%z' or lower(doc.uid)->'$.v[2]' like '%test%z'");
	}

	@Test
	public void checkLowerFilters() throws SearchException {
		Filter userUidFilter1 = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test");

		ConvertedExpression expressionUserUid1 = simpleConverter.convertToSqlFilter(null, userUidFilter1, null);

		String queryUserUid1 = toSelectSQL(expressionUserUid1);
		assertEquals(queryUserUid1, "select doc.`*` from `table` as doc where lower(doc.uid) = 'test'");
	}

	@Test
	public void checkMultivaluedLowerFilters() throws SearchException {
		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test").multiValued();

		ConvertedExpression expressionUserUid = simpleConverter.convertToSqlFilter(null, userUidFilter, null);

		String queryUserUid = toSelectSQL(expressionUserUid);
		assertEquals(queryUserUid, "select doc.`*` from `table` as doc where JSON_CONTAINS(lower(doc.uid)->'$.v', CAST('[\"test\"]' AS JSON))");
	}

	@Test
	public void checkNotFilters() throws SearchException {
		Filter notFilter1 = Filter.createNOTFilter(Filter.createLessOrEqualFilter("age", 23));

		ConvertedExpression expressionNot1 = simpleConverter.convertToSqlFilter(null, notFilter1, null);

		String queryUserUid1 = toSelectSQL(expressionNot1);
		assertEquals(queryUserUid1, "select doc.`*` from `table` as doc where not doc.age <= 23");

		Filter notFilter2 = Filter.createNOTFilter(Filter.createANDFilter(Filter.createLessOrEqualFilter("age", 23), Filter.createGreaterOrEqualFilter("age", 25)));

		ConvertedExpression expressionNot2 = simpleConverter.convertToSqlFilter(null, notFilter2, null);

		String queryUserUid2 = toSelectSQL(expressionNot2);
		assertEquals(queryUserUid2, "select doc.`*` from `table` as doc where not (doc.age <= 23 and doc.age >= 25)");
	}

	@Test
	public void checkAndFilters() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterPresence1 = Filter.createPresenceFilter("mail");
		Filter filterLe1 = Filter.createLessOrEqualFilter("age", 23);
		Filter filterAnd1 = Filter.createANDFilter(filterPresence1, filterEq1, filterLe1);
		ConvertedExpression expressionAnd1 = simpleConverter.convertToSqlFilter(null, filterAnd1, null);

		String queryAnd1 = toSelectSQL(expressionAnd1);
		assertEquals(queryAnd1, "select doc.`*` from `table` as doc where doc.mail is not null and doc.uid = 'test' and doc.age <= 23");
	}

	@Test
	public void checkOrFilters() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterPresence1 = Filter.createPresenceFilter("mail");
		Filter filterLe1 = Filter.createLessOrEqualFilter("age", 23);
		Filter filterOr1 = Filter.createORFilter(filterPresence1, filterEq1, filterLe1);
		ConvertedExpression expressionAnd1 = simpleConverter.convertToSqlFilter(null, filterOr1, null);

		String queryAnd1 = toSelectSQL(expressionAnd1);
		assertEquals(queryAnd1, "select doc.`*` from `table` as doc where doc.mail is not null or doc.uid = 'test' or doc.age <= 23");
	}

	@Test
	public void checkOrJoinFilters() throws SearchException {
		// And with join
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterEq2 = Filter.createEqualityFilter("uid", "test2");
		Filter filterEq3 = Filter.createEqualityFilter("uid", "test3");
		Filter filterOr1 = Filter.createORFilter(filterEq1, filterEq2, filterEq3).multiValued(false);
		ConvertedExpression expressionOr1 = simpleConverter.convertToSqlFilter(null, filterOr1, null);

		String queryOr1 = toSelectSQL(expressionOr1);
		assertEquals(queryOr1, "select doc.`*` from `table` as doc where doc.uid in ('test', 'test2', 'test3')");

		Filter filterOr2 = Filter.createORFilter(filterEq1, filterEq2, filterEq3);
		ConvertedExpression expressionOr2 = simpleConverter.convertToSqlFilter(null, filterOr2, null);

		String queryOr2 = toSelectSQL(expressionOr2);
		assertEquals(queryOr2, "select doc.`*` from `table` as doc where doc.uid = 'test' or doc.uid = 'test2' or doc.uid = 'test3'");
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
        
		ConvertedExpression expression = simpleConverter.convertToSqlFilter(null, filter, null);
		String query = toSelectSQL(expression);
		assertEquals(query, "select doc.`*` from `table` as doc where (lower(doc.description) like '%test_value%' or lower(doc.displayName) like '%test_value%') and doc.jansScrTyp = 'person_authentication'");
	}

	private String toSelectSQL(ConvertedExpression convertedExpression) {
		SQLQuery sqlQuery = (SQLQuery) new SQLQuery(configuration).select(allPath).from(tableAlieasPath)
				.where((Predicate) convertedExpression.expression());
		sqlQuery.setUseLiterals(true);

		String queryStr = sqlQuery.getSQL().getSQL().replace("\n", " ");

		return queryStr;
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
