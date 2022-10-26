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
import io.jans.orm.search.filter.FilterProcessor;
import io.jans.orm.sql.dsl.template.MySQLJsonTemplates;
import io.jans.orm.sql.impl.SqlFilterConverter;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.operation.impl.SqlOperationServiceImpl;

@SuppressWarnings({ "rawtypes", "unchecked"})
public class SqlFilterConverterCheckExcludeFilterTest {

	private SqlFilterConverter simpleConverter;
	private FilterProcessor filterProcessor;
	private Path<Object> tablePath;
	private Path<Object> docAlias;
	private SimpleExpression<Object> tableAlieasPath;
	private StringPath allPath;
	private SQLTemplates sqlTemplates;
	private Configuration configuration;

	@BeforeClass
	public void init() {
		this.simpleConverter = new SqlFilterConverter(new SqlOperationServiceImpl(null, new SqlConnectionProvider(null)));
		this.filterProcessor = new FilterProcessor();
		this.tablePath = ExpressionUtils.path(Object.class, "table");
		this.docAlias = ExpressionUtils.path(Object.class, "doc");
		this.tableAlieasPath = Expressions.as(tablePath, docAlias);
		this.allPath = Expressions.stringPath(docAlias, "*");

//		this.sqlTemplates = MySQLTemplates.builder().printSchema().build();
		this.sqlTemplates = MySQLJsonTemplates.builder().printSchema().build();
		this.configuration = new Configuration(sqlTemplates);
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
		ConvertedExpression expression1 = simpleConverter.convertToSqlFilter(null, filter1, null);

		String query1 = toSelectSQL(expression1);
		assertEquals(query1, "select doc.`*` from `table` as doc where doc.objectClass = 'jansPerson' and (doc.uid = 'test' and lower(doc.uid) = 'test' and doc.objectClass = 'jansPerson' and (doc.uid = 'test' and lower(doc.uid) = 'test' and doc.objectClass = 'jansPerson' and JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON))) and JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON)))");

		Filter filter2 = filterProcessor.excludeFilter(filter1, filterEq3);

		ConvertedExpression expression2 = simpleConverter.convertToSqlFilter(null, filter2, null);

		String query2 = toSelectSQL(expression2);
		assertEquals(query2, "select doc.`*` from `table` as doc where doc.uid = 'test' and lower(doc.uid) = 'test' and (doc.uid = 'test' and lower(doc.uid) = 'test' and JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON))) and JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON))");

		Filter filter3 = filterProcessor.excludeFilter(filter1, Filter.createEqualityFilter("objectClass", null));

		ConvertedExpression expression3 = simpleConverter.convertToSqlFilter(null, filter3, null);

		String query3 = toSelectSQL(expression3);
		assertEquals(query3, "select doc.`*` from `table` as doc where doc.uid = 'test' and lower(doc.uid) = 'test' and (doc.uid = 'test' and lower(doc.uid) = 'test' and JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON))) and JSON_CONTAINS(doc.added->'$.v', CAST('[\"2020-12-16T14:58:18.398\"]' AS JSON))");
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
