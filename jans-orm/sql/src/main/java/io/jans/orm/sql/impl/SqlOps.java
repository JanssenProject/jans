package io.jans.orm.sql.impl;

import com.querydsl.core.types.Operator;

public enum SqlOps implements Operator {

	JSON_CONTAINS(Object.class),
	JSON_EXTRACT(Object.class);

    private final Class<?> type;

    private SqlOps(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

}
