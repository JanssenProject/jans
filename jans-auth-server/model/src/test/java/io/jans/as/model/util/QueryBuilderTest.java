package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class QueryBuilderTest extends BaseTest {

    @Test
    public void appendIfNotNull_nullValue_builderEmpty() {
        showTitle("appendIfNotNull_nullValue_builderEmpty");
        QueryBuilder queryBuilder = QueryBuilder.instance();
        assertNotNull(queryBuilder);
        assertNotNull(queryBuilder.getBuilder());
        queryBuilder.appendIfNotNull("key1", null);
        assertTrue(queryBuilder.getBuilder().length() == 0);
    }

    @Test
    public void appendIfNotNull_validValue_builderNoEmpty() {
        showTitle("appendIfNotNull_validValue_builderNoEmpty");
        QueryBuilder queryBuilder = QueryBuilder.instance();
        assertNotNull(queryBuilder);
        assertNotNull(queryBuilder.getBuilder());
        queryBuilder.appendIfNotNull("key1", "value1");
        assertTrue(queryBuilder.getBuilder().length() > 0);
    }

    @Test
    public void build_validAppends_validStringBuilt() {
        showTitle("build_validAppends_validStringBuilt");
        QueryBuilder queryBuilder = QueryBuilder.instance();
        assertNotNull(queryBuilder);
        assertNotNull(queryBuilder.getBuilder());
        queryBuilder.append("key1", "value1");
        queryBuilder.append("key2", "value2");
        queryBuilder.append("key3", "value3");
        assertEquals(queryBuilder.build(), "key1=value1&key2=value2&key3=value3");
        assertEquals(queryBuilder.toString(), "key1=value1&key2=value2&key3=value3");
    }

}
