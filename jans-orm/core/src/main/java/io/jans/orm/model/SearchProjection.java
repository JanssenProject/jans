/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.orm.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.jans.orm.util.StringHelper;

/**
 * Describes the SELECT-clause shape of a server-side GROUP BY or DISTINCT query.
 *
 * A projection is either grouping ({@link #groupBy(String...)}) or distinct
 * ({@link #distinct(String...)}). Grouping projections may carry aggregates
 * (COUNT/SUM/MIN/MAX/AVG); distinct projections may not. Result ordering is
 * restricted to projection attributes and aggregate aliases.
 *
 * @author Yuriy Movchan
 */
public class SearchProjection implements Serializable {

    private static final long serialVersionUID = 4525109217833394852L;

    public enum AggregateType {
        COUNT, SUM, MIN, MAX, AVG
    }

    public static class Aggregate implements Serializable {

        private static final long serialVersionUID = 6412319263817465913L;

        private final AggregateType type;
        private final String attributeName;
        private final String alias;

        Aggregate(AggregateType type, String attributeName, String alias) {
            this.type = type;
            this.attributeName = attributeName;
            this.alias = alias;
        }

        public AggregateType getType() {
            return type;
        }

        /**
         * Aggregated attribute. Null for COUNT(*).
         */
        public String getAttributeName() {
            return attributeName;
        }

        public String getAlias() {
            return alias;
        }

        @Override
        public String toString() {
            return String.format("%s(%s) as %s", type, (attributeName == null) ? "*" : attributeName, alias);
        }
    }

    public static class ProjectionSort implements Serializable {

        private static final long serialVersionUID = 7093219531412447312L;

        private final String name;
        private final SortOrder sortOrder;

        ProjectionSort(String name, SortOrder sortOrder) {
            this.name = name;
            this.sortOrder = sortOrder;
        }

        /**
         * Projection attribute name or aggregate alias.
         */
        public String getName() {
            return name;
        }

        public SortOrder getSortOrder() {
            return sortOrder;
        }

        @Override
        public String toString() {
            return name + " " + sortOrder.getShortValue();
        }
    }

    private final boolean distinct;
    private final String[] attributes;
    private final List<Aggregate> aggregates = new ArrayList<Aggregate>();
    private final List<ProjectionSort> orderBy = new ArrayList<ProjectionSort>();

    private SearchProjection(boolean distinct, String[] attributes) {
        if ((attributes == null) || (attributes.length == 0)) {
            throw new IllegalArgumentException("Projection requires at least one attribute");
        }
        for (String attribute : attributes) {
            if (StringHelper.isEmpty(attribute)) {
                throw new IllegalArgumentException("Projection attribute name can't be empty");
            }
        }
        this.distinct = distinct;
        this.attributes = attributes.clone();
    }

    /**
     * Group result rows by the specified attributes.
     */
    public static SearchProjection groupBy(String... attributes) {
        return new SearchProjection(false, attributes);
    }

    /**
     * Select distinct combinations of the specified attributes.
     */
    public static SearchProjection distinct(String... attributes) {
        return new SearchProjection(true, attributes);
    }

    public SearchProjection count() {
        return count("total");
    }

    public SearchProjection count(String alias) {
        return addAggregate(new Aggregate(AggregateType.COUNT, null, alias));
    }

    public SearchProjection sum(String attributeName) {
        return sum(attributeName, "sum_" + attributeName);
    }

    public SearchProjection sum(String attributeName, String alias) {
        return addAggregate(new Aggregate(AggregateType.SUM, requireAttribute(attributeName), alias));
    }

    public SearchProjection min(String attributeName) {
        return min(attributeName, "min_" + attributeName);
    }

    public SearchProjection min(String attributeName, String alias) {
        return addAggregate(new Aggregate(AggregateType.MIN, requireAttribute(attributeName), alias));
    }

    public SearchProjection max(String attributeName) {
        return max(attributeName, "max_" + attributeName);
    }

    public SearchProjection max(String attributeName, String alias) {
        return addAggregate(new Aggregate(AggregateType.MAX, requireAttribute(attributeName), alias));
    }

    public SearchProjection avg(String attributeName) {
        return avg(attributeName, "avg_" + attributeName);
    }

    public SearchProjection avg(String attributeName, String alias) {
        return addAggregate(new Aggregate(AggregateType.AVG, requireAttribute(attributeName), alias));
    }

    /**
     * Order result rows by a projection attribute or an aggregate alias.
     */
    public SearchProjection orderBy(String attributeOrAlias, SortOrder sortOrder) {
        if (StringHelper.isEmpty(attributeOrAlias)) {
            throw new IllegalArgumentException("Order attribute name can't be empty");
        }
        orderBy.add(new ProjectionSort(attributeOrAlias, (sortOrder == null) ? SortOrder.ASCENDING : sortOrder));
        return this;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public String[] getAttributes() {
        return attributes.clone();
    }

    public List<Aggregate> getAggregates() {
        return new ArrayList<Aggregate>(aggregates);
    }

    public List<ProjectionSort> getOrderBy() {
        return new ArrayList<ProjectionSort>(orderBy);
    }

    public boolean hasAggregates() {
        return !aggregates.isEmpty();
    }

    private SearchProjection addAggregate(Aggregate aggregate) {
        if (distinct) {
            throw new IllegalStateException("Aggregates are not allowed on a distinct projection");
        }
        if (StringHelper.isEmpty(aggregate.getAlias())) {
            throw new IllegalArgumentException("Aggregate alias can't be empty");
        }

        Set<String> usedNames = new LinkedHashSet<String>();
        for (String attribute : attributes) {
            usedNames.add(StringHelper.toLowerCase(attribute));
        }
        for (Aggregate existingAggregate : aggregates) {
            usedNames.add(StringHelper.toLowerCase(existingAggregate.getAlias()));
        }
        if (usedNames.contains(StringHelper.toLowerCase(aggregate.getAlias()))) {
            throw new IllegalArgumentException(
                    String.format("Aggregate alias '%s' conflicts with a projection attribute or another alias", aggregate.getAlias()));
        }

        aggregates.add(aggregate);
        return this;
    }

    private static String requireAttribute(String attributeName) {
        if (StringHelper.isEmpty(attributeName)) {
            throw new IllegalArgumentException("Aggregate attribute name can't be empty");
        }
        return attributeName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(distinct ? "distinct(" : "groupBy(");
        sb.append(String.join(", ", attributes)).append(")");
        for (Aggregate aggregate : aggregates) {
            sb.append(".").append(aggregate);
        }
        for (ProjectionSort sort : orderBy) {
            sb.append(" orderBy ").append(sort);
        }
        return sb.toString();
    }

}
