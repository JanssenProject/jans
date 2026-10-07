package io.jans.orm.sql.impl.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;

import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.dsl.template.MySQLJsonTemplates;
import io.jans.orm.sql.dsl.template.PostgreSQLJsonTemplates;
import io.jans.orm.sql.impl.SqlFilterConverter;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.impl.SqlAggregationQueryBuilder;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.operation.impl.SqlOperationServiceImpl;

/**
 * @author Yuriy Movchan
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SqlAggregationQueryBuilderTest {

	private SqlAggregationQueryBuilder builder;
	private SqlFilterConverter filterConverter;
	private TableMapping tableMapping;
	private Path<Object> tablePath;
	private Path<Object> docAlias;
	private SimpleExpression<Object> tableAliasPath;
	private Configuration mySqlConfiguration;
	private Configuration postgresConfiguration;

	@BeforeClass
	public void init() {
		SqlOperationServiceImpl operationService = new SqlOperationServiceImpl(null, new SqlConnectionProvider(null));
		this.builder = new SqlAggregationQueryBuilder(operationService);
		this.filterConverter = new SqlFilterConverter(operationService);

		Map<String, AttributeType> columTypes = new HashMap<String, AttributeType>();
		columTypes.put("ou", new AttributeType("ou", "ou", "varchar"));
		columTypes.put("jansstatus", new AttributeType("jansStatus", "jansstatus", "varchar"));
		columTypes.put("logincount", new AttributeType("loginCount", "logincount", "int"));
		columTypes.put("mail", new AttributeType("mail", "mail", "varchar"));
		columTypes.put("uid", new AttributeType("uid", "uid", "varchar"));
		columTypes.put("jansextuid", new AttributeType("jansExtUid", "jansextuid", "json", Boolean.TRUE));
		columTypes.put("doc_id", new AttributeType("doc_id", "doc_id", "varchar"));
		this.tableMapping = new TableMapping("", "table", "jansPerson", columTypes);

		this.tablePath = ExpressionUtils.path(Object.class, "table");
		this.docAlias = ExpressionUtils.path(Object.class, "doc");
		this.tableAliasPath = Expressions.as(tablePath, docAlias);

		SQLTemplates mySqlTemplates = MySQLJsonTemplates.builder().printSchema().build();
		this.mySqlConfiguration = new Configuration(mySqlTemplates);

		SQLTemplates postgresTemplates = PostgreSQLJsonTemplates.builder().printSchema().quote().build();
		this.postgresConfiguration = new Configuration(postgresTemplates);
	}

	@Test
	public void checkGroupByWithCount() throws SearchException {
		SearchProjection projection = SearchProjection.groupBy("ou", "jansStatus").count();
		SqlAggregationQueryBuilder.Result parts = builder.build(tableMapping, projection);

		String query = toSelectSQL(mySqlConfiguration, parts, projection, null);
		assertEquals(query,
				"select doc.ou, doc.jansStatus, count(*) as total from `table` as doc group by doc.ou, doc.jansStatus order by doc.ou asc, doc.jansStatus asc");
	}

	@Test
	public void checkGroupByWithCountAndSumAndFilter() throws SearchException {
		Filter filter = Filter.createEqualityFilter("jansStatus", "active");
		ConvertedExpression expression = filterConverter.convertToSqlFilter(null, filter, null);

		SearchProjection projection = SearchProjection.groupBy("ou").count().sum("loginCount");
		SqlAggregationQueryBuilder.Result parts = builder.build(tableMapping, projection);

		String query = toSelectSQL(mySqlConfiguration, parts, projection, (Predicate) expression.expression());
		assertEquals(query,
				"select doc.ou, count(*) as total, sum(doc.loginCount) as sum_loginCount from `table` as doc where doc.jansStatus = 'active' group by doc.ou order by doc.ou asc");
	}

	@Test
	public void checkDistinctHasNoDnAndDocId() throws SearchException {
		SearchProjection projection = SearchProjection.distinct("mail", "uid");
		SqlAggregationQueryBuilder.Result parts = builder.build(tableMapping, projection);

		String query = toSelectSQL(mySqlConfiguration, parts, projection, null);
		assertEquals(query, "select distinct doc.mail, doc.uid from `table` as doc order by doc.mail asc, doc.uid asc");
		assertTrue(!query.contains("dn"));
		assertTrue(!query.contains("doc_id"));
	}

	@Test
	public void checkCountWrapperShape() throws SearchException {
		SearchProjection projection = SearchProjection.groupBy("ou").count();
		SqlAggregationQueryBuilder.Result parts = builder.build(tableMapping, projection);

		SQLQuery innerQuery = (SQLQuery) new SQLQuery(mySqlConfiguration).select(parts.selectExpression())
				.from(tableAliasPath).groupBy(parts.getGroupBy());
		innerQuery.setUseLiterals(true);

		SQLQuery countQuery = (SQLQuery) new SQLQuery(mySqlConfiguration)
				.select(Expressions.as(ExpressionUtils.count(Wildcard.all), "TOTAL"))
				.from(innerQuery, ExpressionUtils.path(String.class, "doc_inner"));
		countQuery.setUseLiterals(true);

		String query = countQuery.getSQL().getSQL().replace("\n", " ");
		assertEquals(query,
				"select count(*) as TOTAL from (select doc.ou, count(*) as total from `table` as doc group by doc.ou) as doc_inner");
	}

	@Test
	public void checkOrderByAggregateAlias() throws SearchException {
		SearchProjection projection = SearchProjection.groupBy("ou").count().orderBy("total", SortOrder.DESCENDING);
		SqlAggregationQueryBuilder.Result parts = builder.build(tableMapping, projection);

		String query = toSelectSQL(mySqlConfiguration, parts, projection, null);
		assertEquals(query,
				"select doc.ou, count(*) as total from `table` as doc group by doc.ou order by total desc");
	}

	@Test
	public void checkPostgresGroupBy() throws SearchException {
		SearchProjection projection = SearchProjection.groupBy("ou").count();
		SqlAggregationQueryBuilder.Result parts = builder.build(tableMapping, projection);

		String query = toSelectSQL(postgresConfiguration, parts, projection, null);
		assertEquals(query,
				"select \"doc\".\"ou\", count(*) as \"total\" from \"table\" as \"doc\" group by \"doc\".\"ou\" order by \"doc\".\"ou\" asc");
	}

	@Test
	public void checkUnknownOrderByRejected() {
		SearchProjection projection = SearchProjection.groupBy("ou").count().orderBy("mail", SortOrder.ASCENDING);
		try {
			builder.build(tableMapping, projection);
			fail("Order by non-projection attribute must be rejected");
		} catch (SearchException ex) {
			assertTrue(ex.getMessage().contains("mail"));
		}
	}

	@Test
	public void checkJsonColumnRejected() {
		try {
			builder.build(tableMapping, SearchProjection.groupBy("jansExtUid"));
			fail("GROUP BY over JSON column must be rejected");
		} catch (SearchException ex) {
			assertTrue(ex.getMessage().contains("jansExtUid"));
		}

		try {
			builder.build(tableMapping, SearchProjection.groupBy("ou").sum("jansExtUid"));
			fail("SUM over JSON column must be rejected");
		} catch (SearchException ex) {
			assertTrue(ex.getMessage().contains("jansExtUid"));
		}
	}

	@Test
	public void checkUnknownAttributeRejected() {
		try {
			builder.build(tableMapping, SearchProjection.groupBy("missingAttribute"));
			fail("Unknown attribute must be rejected");
		} catch (SearchException ex) {
			assertTrue(ex.getMessage().contains("missingAttribute"));
		}
	}

	@Test
	public void checkInternalColumnRejected() {
		try {
			builder.build(tableMapping, SearchProjection.groupBy("doc_id"));
			fail("Internal column must be rejected");
		} catch (SearchException ex) {
			assertTrue(ex.getMessage().contains("doc_id"));
		}
	}

	@Test
	public void checkProjectionValidation() {
		try {
			SearchProjection.distinct("mail").count();
			fail("Aggregates on distinct projection must be rejected");
		} catch (IllegalStateException ex) {
			// expected
		}

		try {
			SearchProjection.groupBy();
			fail("Empty projection must be rejected");
		} catch (IllegalArgumentException ex) {
			// expected
		}

		try {
			SearchProjection.groupBy("ou").count("ou");
			fail("Alias colliding with projection attribute must be rejected");
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	private String toSelectSQL(Configuration configuration, SqlAggregationQueryBuilder.Result parts,
			SearchProjection projection, Predicate where) {
		SQLQuery sqlQuery = (SQLQuery) new SQLQuery(configuration).select(parts.selectExpression()).from(tableAliasPath);
		if (where != null) {
			sqlQuery = (SQLQuery) sqlQuery.where(where);
		}
		if (projection.isDistinct()) {
			sqlQuery = (SQLQuery) sqlQuery.distinct();
		} else {
			sqlQuery = (SQLQuery) sqlQuery.groupBy(parts.getGroupBy());
		}
		sqlQuery = (SQLQuery) sqlQuery.orderBy(parts.getOrderBy());
		sqlQuery.setUseLiterals(true);

		return sqlQuery.getSQL().getSQL().replace("\n", " ");
	}

}
