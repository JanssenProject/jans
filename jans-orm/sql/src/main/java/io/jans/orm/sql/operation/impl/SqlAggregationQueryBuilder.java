/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;

import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.model.SearchProjection.Aggregate;
import io.jans.orm.model.SearchProjection.ProjectionSort;
import io.jans.orm.model.SortOrder;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.util.StringHelper;

/**
 * Validates a {@link SearchProjection} against a table mapping and assembles the
 * QueryDSL select / groupBy / orderBy parts for a server-side GROUP BY or DISTINCT query.
 *
 * The select list contains exactly the projection columns and aggregates — no forced
 * dn/doc_id — so generated queries stay valid under MySQL ONLY_FULL_GROUP_BY and
 * PostgreSQL strictness, and DISTINCT keeps its meaning.
 *
 * @author Yuriy Movchan
 */
public class SqlAggregationQueryBuilder {

    private final SqlOperationService operationService;
    private final Path<String> docAlias = ExpressionUtils.path(String.class, SqlOperationService.DOC_ALIAS);

    public SqlAggregationQueryBuilder(SqlOperationService operationService) {
        this.operationService = operationService;
    }

    public static class Result {

        private final List<Expression<?>> select;
        private final Expression<?>[] groupBy;
        private final OrderSpecifier<?>[] orderBy;

        Result(List<Expression<?>> select, Expression<?>[] groupBy, OrderSpecifier<?>[] orderBy) {
            this.select = select;
            this.groupBy = groupBy;
            this.orderBy = orderBy;
        }

        public Expression<?> selectExpression() {
            return Expressions.list(select.toArray(new Expression<?>[0]));
        }

        public Expression<?>[] getGroupBy() {
            return groupBy;
        }

        public OrderSpecifier<?>[] getOrderBy() {
            return orderBy;
        }
    }

    public Result build(TableMapping tableMapping, SearchProjection projection) throws SearchException {
        // Resolve and validate projection columns
        Map<String, String> attributeToColumn = new LinkedHashMap<String, String>();
		for (String attributeName : projection.getAttributes()) {
			attributeToColumn.put(StringHelper.toLowerCase(attributeName), resolveColumn(tableMapping, attributeName));
		}

        List<Expression<?>> select = new ArrayList<Expression<?>>();
        List<Expression<?>> groupBy = new ArrayList<Expression<?>>();
        for (String column : attributeToColumn.values()) {
            Path<Object> columnPath = Expressions.path(Object.class, docAlias, column);
            select.add(columnPath);
            groupBy.add(columnPath);
        }

        Map<String, String> aliases = new LinkedHashMap<String, String>();
        for (Aggregate aggregate : projection.getAggregates()) {
            select.add(buildAggregateExpression(tableMapping, aggregate));
            aliases.put(StringHelper.toLowerCase(aggregate.getAlias()), aggregate.getAlias());
        }

        OrderSpecifier<?>[] orderBy = buildOrderBy(tableMapping, projection, attributeToColumn, aliases);

        Expression<?>[] groupByArray = projection.isDistinct() ? new Expression<?>[0]
                : groupBy.toArray(new Expression<?>[0]);

        return new Result(select, groupByArray, orderBy);
    }

    private Expression<?> buildAggregateExpression(TableMapping tableMapping, Aggregate aggregate) throws SearchException {
        if (aggregate.getAttributeName() == null) {
            return Expressions.as(ExpressionUtils.count(Wildcard.all), aggregate.getAlias());
        }

        String column = resolveColumn(tableMapping, aggregate.getAttributeName());
        switch (aggregate.getType()) {
        case SUM:
            return Expressions.as(Expressions.numberPath(BigDecimal.class, docAlias, column).sum(), aggregate.getAlias());
        case AVG:
            return Expressions.as(Expressions.numberPath(BigDecimal.class, docAlias, column).avg(), aggregate.getAlias());
        case MIN:
            return Expressions.as(Expressions.comparablePath(String.class, docAlias, column).min(), aggregate.getAlias());
        case MAX:
            return Expressions.as(Expressions.comparablePath(String.class, docAlias, column).max(), aggregate.getAlias());
        default:
            throw new SearchException(String.format("Unsupported aggregate type '%s'", aggregate.getType()));
        }
    }

    private OrderSpecifier<?>[] buildOrderBy(TableMapping tableMapping, SearchProjection projection,
            Map<String, String> attributeToColumn, Map<String, String> aliases) throws SearchException {
        List<ProjectionSort> requestedOrder = projection.getOrderBy();
        List<OrderSpecifier<?>> orderBy = new ArrayList<OrderSpecifier<?>>();

        if (requestedOrder.isEmpty()) {
            // Mandatory deterministic order so LIMIT/OFFSET paging over groups is stable;
            // projection columns are always legal sort targets under strict SQL modes
            for (String column : attributeToColumn.values()) {
                orderBy.add(new OrderSpecifier<String>(Order.ASC, Expressions.stringPath(docAlias, column)));
            }
            return orderBy.toArray(new OrderSpecifier<?>[0]);
        }

        for (ProjectionSort sort : requestedOrder) {
            Order order = (SortOrder.DESCENDING == sort.getSortOrder()) ? Order.DESC : Order.ASC;
            String column = attributeToColumn.get(StringHelper.toLowerCase(sort.getName()));
            if (column != null) {
                orderBy.add(new OrderSpecifier<String>(order, Expressions.stringPath(docAlias, column)));
            } else if (aliases.containsKey(StringHelper.toLowerCase(sort.getName()))) {
                // Output-column reference; legal in ORDER BY on MySQL/MariaDB/PostgreSQL
                orderBy.add(new OrderSpecifier<String>(order, Expressions.stringPath(aliases.get(StringHelper.toLowerCase(sort.getName())))));
            } else {
                throw new SearchException(String.format(
                        "Order attribute '%s' must be a projection attribute or an aggregate alias", sort.getName()));
            }
        }

        return orderBy.toArray(new OrderSpecifier<?>[0]);
    }

    private String resolveColumn(TableMapping tableMapping, String attributeName) throws SearchException {
        Map<String, AttributeType> columTypes = tableMapping.getColumTypes();
        if (columTypes == null) {
            throw new SearchException(String.format("Unknown table '%s' and it's column '%s'",
                    tableMapping.getTableName(), attributeName));
        }

        AttributeType attributeType = columTypes.get(StringHelper.toLowerCase(attributeName));
        if (attributeType == null) {
            throw new SearchException(String.format("Unknown column name '%s' in table '%s'",
                    attributeName, tableMapping.getTableName()));
        }

        String column = (attributeType.getDefName() != null) ? attributeType.getDefName() : attributeName;

        if (SqlOperationService.DOC_ID.equalsIgnoreCase(column) || SqlOperationService.ID.equalsIgnoreCase(column)) {
            // The result mapper drops these columns as internal, which would silently lose data
            throw new SearchException(String.format(
                    "Internal column '%s' can't be used in GROUP BY/DISTINCT/aggregate", column));
        }

        if ((operationService != null) && operationService.isJsonColumn(tableMapping.getTableName(), attributeType.getType())) {
            throw new SearchException(String.format(
                    "Attribute '%s' is multi-valued (JSON column) and can't be used in GROUP BY/DISTINCT/aggregate", attributeName));
        }

        return column;
    }

}
