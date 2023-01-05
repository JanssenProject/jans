/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.dsl.template;

import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLTemplates;

import io.jans.orm.sql.impl.SqlOps;

/**
 * MySQL DSL templates for JSON support
 *
 * @author Yuriy Movchan Date: 01/27/2021
 */
public class MySQLJsonTemplates extends MySQLTemplates {
	
    public static Builder builder() {
        return new Builder() {
            @Override
            protected SQLTemplates build(char escape, boolean quote) {
                return new MySQLJsonTemplates(escape, quote);
            }
        };
    }

    public MySQLJsonTemplates(char escape, boolean quote) {
		super(escape, quote);

		add(SqlOps.JSON_CONTAINS, "JSON_CONTAINS({0}->{2}, CAST({1} AS JSON))");
		add(SqlOps.JSON_EXTRACT, "{0}->{1}");
	}

}