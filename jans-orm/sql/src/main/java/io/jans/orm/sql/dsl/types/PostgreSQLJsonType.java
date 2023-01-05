/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.dsl.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.postgresql.util.PGobject;

import com.querydsl.sql.types.AbstractType;

import io.jans.orm.sql.model.JsonString;

/**
 * PostgreSQL JSON column type support
 *
 * @author Yuriy Movchan Date: 09/01/2022
 */

public class PostgreSQLJsonType extends AbstractType<JsonString> {

    public PostgreSQLJsonType() {
        super(Types.JAVA_OBJECT);
    }

    public PostgreSQLJsonType(int type) {
        super(type);
    }

    @Override
    public JsonString getValue(ResultSet rs, int startIndex) throws SQLException {
        return new JsonString(rs.getString(startIndex));
    }

    @Override
    public Class<JsonString> getReturnedClass() {
        return JsonString.class;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, JsonString value)
            throws SQLException {
        final PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        jsonObject.setValue(value.getValue());
        
        st.setObject(startIndex, jsonObject);
    }
}
