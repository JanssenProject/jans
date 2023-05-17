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
 * MariaDB MySQL DSL templates for JSON support
 *
 * @author Yuriy Movchan Date: 05/17/2023
 */
public class MariaDBJsonTemplates extends MySQLTemplates {
	
    public static Builder builder() {
        return new Builder() {
            @Override
            protected SQLTemplates build(char escape, boolean quote) {
                return new MariaDBJsonTemplates(escape, quote);
            }
        };
    }

    public MariaDBJsonTemplates(char escape, boolean quote) {
		super(escape, quote);

		add(SqlOps.JSON_CONTAINS, "JSON_CONTAINS({0}, {1}, {2})");
		add(SqlOps.JSON_EXTRACT, "JSON_EXTRACT({0}, {1})");
	}

}