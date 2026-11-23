package io.jans.scim.service.antlr.scimFilter;

import io.jans.scim.model.scim2.AttributeDefinition.Type;
import io.jans.scim.service.antlr.scimFilter.enums.CompValueType;
import io.jans.scim.service.antlr.scimFilter.enums.ScimOperator;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class SubFilterGeneratorCaseTest {

    private static final String VALUE = "\"John.Doe@Example.com\"";

    private Filter build(boolean ldap, ScimOperator op, Boolean multiValued, boolean caseExact, String subAttribute) {
        return new SubFilterGenerator(ldap)
                .build(subAttribute, "uid", VALUE, Type.STRING, CompValueType.STRING, op, multiValued, caseExact)
                .getFirst();
    }

    private void assertLowercaseEquality(Filter f) {
        assertEquals(f.getType(), FilterType.EQUALITY);
        assertEquals(f.getFilters().length, 1);
        assertEquals(f.getFilters()[0].getType(), FilterType.LOWERCASE);
        assertEquals(f.getFilters()[0].getAttributeName(), "uid");
        assertEquals(f.getAssertionValue(), "john.doe@example.com");
    }

    private void assertPlainEquality(Filter f) {
        assertEquals(f.getType(), FilterType.EQUALITY);
        assertEquals(f.getAttributeName(), "uid");
        assertEquals(f.getAssertionValue(), "John.Doe@Example.com");
    }

    @Test
    public void eqNotCaseExactUsesLowercase() {
        assertLowercaseEquality(build(false, ScimOperator.EQUAL, false, false, null));
    }

    @Test
    public void neNotCaseExactNegatesLowercase() {
        Filter f = build(false, ScimOperator.NOT_EQUAL, false, false, null);
        assertEquals(f.getType(), FilterType.NOT);
        assertLowercaseEquality(f.getFilters()[0]);
    }

    @Test
    public void eqCaseExactKeepsValue() {
        assertPlainEquality(build(false, ScimOperator.EQUAL, false, true, null));
    }

    @Test
    public void eqLdapBackendUnchanged() {
        assertPlainEquality(build(true, ScimOperator.EQUAL, false, false, null));
    }

    @Test
    public void eqMultivaluedUnchanged() {
        assertPlainEquality(build(false, ScimOperator.EQUAL, true, false, null));
        assertPlainEquality(build(false, ScimOperator.EQUAL, null, false, null));
    }

    @Test
    public void eqSubAttributeStaysSubstring() {
        Filter f = build(false, ScimOperator.EQUAL, false, false, "value");
        assertEquals(f.getType(), FilterType.SUBSTRING);
        assertNull(f.getFilters());
    }

    @Test
    public void legacyOverloadIsCaseExact() {
        Filter f = new SubFilterGenerator(false)
                .build(null, "uid", VALUE, Type.STRING, CompValueType.STRING, ScimOperator.EQUAL, false)
                .getFirst();
        assertPlainEquality(f);
    }
}
