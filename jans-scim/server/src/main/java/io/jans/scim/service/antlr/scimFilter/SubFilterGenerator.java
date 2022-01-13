/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.antlr.scimFilter;

import io.jans.orm.search.filter.Filter;
import io.jans.scim.model.scim2.AttributeDefinition.Type;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.service.antlr.scimFilter.enums.CompValueType;
import io.jans.scim.service.antlr.scimFilter.enums.ScimOperator;
import io.jans.scim.service.antlr.scimFilter.util.FilterUtil;
import io.jans.util.Pair;

import java.util.Optional;

public class SubFilterGenerator {

    Logger log = LogManager.getLogger(getClass());

    private boolean ldapBackend;
    private String error;

    public SubFilterGenerator(boolean ldapBackend) {
        this.ldapBackend = ldapBackend;
    }

    /**
     * Computes a filter based on an atomic (non-divisible) SCIM expression described by the set of parameters passed.
     * If the filter cannot be built, null is returned.
     * @param subAttribute The name of the json property upon which the search is acting. If the search is targetted
     *                     directly upon an attribute, this is null
     * @param attribute The attribute of interest. If the attribute contains json contents, the search can be focused on
     *                  a sub-attribute inside it
     * @param compValue The comparison value (it's found after the operator, e.g "hi" in displayName co "hi")
     * @param attrType The attribute definition type of the attribute
     * @param type See compvalue in ScimFilter.g4 grammar file
     * @param operator The operator
     * @param multiValued Whether the attribute referenced in parameter is multivalued or not (null tries to handle both) 
     * @return The filter built after processing this atomic expression (accompanied with an error string if any)
     */
    public Pair<Filter, String> build(String subAttribute, String attribute, String compValue, Type attrType,
                                      CompValueType type, ScimOperator operator, Boolean multiValued) {
    
        log.debug("Preparing subfilter with attribute={}, subAttribute={}, compValue={}, attrType={}, multiValued={}",
        	attribute, subAttribute, compValue, attrType, multiValued);
        Filter filth = null;
        error = null;

        if (type.equals(CompValueType.NULL)) {
            if (subAttribute == null) {
                //attribute=*
                //attribute IS NOT MISSING
                filth = Filter.createPresenceFilter(attribute).multiValued(multiValued);
                filth = negateIf(filth, operator.equals(ScimOperator.EQUAL));
            } else {
                //attribute=*"subattribute":null*
                //attribute LIKE "%\"subattribute\":null%"
                String sub = String.format("\"%s\":null", subAttribute);
                filth = Filter.createSubstringFilter(attribute, null, new String[]{ sub }, null).multiValued(multiValued);
                filth = negateIf(filth, operator.equals(ScimOperator.NOT_EQUAL));
            }
        } else if (Type.STRING.equals(attrType) || Type.REFERENCE.equals(attrType)) {
            compValue = compValue.substring(1, compValue.length() - 1);     //Drop double quotes
            filth = getSubFilterString(subAttribute, attribute, StringEscapeUtils.unescapeJson(compValue), operator, multiValued);

        } else if (Type.INTEGER.equals(attrType) || Type.DECIMAL.equals(attrType)) {
            filth = getSubFilterNumeric(subAttribute, attribute, compValue, operator, attrType, multiValued);

        } else if (Type.BOOLEAN.equals(attrType)) {
            filth = getSubFilterBoolean(subAttribute, attribute, compValue, operator, multiValued);

        } else if (Type.DATETIME.equals(attrType)) {
            compValue = compValue.substring(1, compValue.length() - 1);
            //Dates do not have characters to escape...
            filth = getSubFilterDateTime(subAttribute, attribute, compValue, operator, multiValued);

        }
        log.trace("getSubFilter. {}", Optional.ofNullable(filth).map(Filter::toString).orElse(null));
        return new Pair<>(filth, error);

    }

    private Filter getSubFilterString(String subAttribute, String attribute, String value, ScimOperator operator, Boolean multivalued) {

        Filter subfilter = null;
        log.trace("getSubFilterString");

        switch (operator) {
            case EQUAL:
            case NOT_EQUAL:
                if (subAttribute == null) {
                    //attribute=value
                    //attribute="value"
                    subfilter = Filter.createEqualityFilter(//Filter.createLowercaseFilter(attribute), 
                    		//ldapBackend ? value : value.toLowerCase()
                    		attribute, value).multiValued(multivalued);
                } else {
                    //attribute=*"subattribute":"value"*
                    //attribute LIKE "%\"subAttribute\":\"value\"%"
                    String sub = String.format("\"%s\":\"%s\"", subAttribute, value);
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub }, null)
                    				.multiValued(multivalued);
                }
                subfilter = negateIf(subfilter, operator.equals(ScimOperator.NOT_EQUAL));
                break;
            case CONTAINS:
                if (subAttribute == null) {
                    //attribute=*value*
                    //attribute LIKE "%value%"
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ value }, null)
                    				.multiValued(multivalued);
                } else {
                    //attribute=*"subAttribute":"*value*"*
                    //attribute LIKE "%\"subAttribute\":\"%value%\"%"
                    String sub = String.format("\"%s\":\"", subAttribute);
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub, value, "\"" }, null)
                    				.multiValued(multivalued);
                }
                break;
            case STARTS_WITH:
                if (subAttribute == null) {
                    //attribute=value*
                    //attribute LIKE "value%"
                    subfilter = Filter.createSubstringFilter(attribute, value, null, null)
                    				.multiValued(multivalued);
                } else {
                    //attribute=*"subAttribute":"value*"*
                    //attribute LIKE "%\"subAttribute\":\"value%\"%"
                    String sub = String.format("\"%s\":\"%s", subAttribute, value);
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub, "\"" }, null)
                    				.multiValued(multivalued);
                }
                break;
            case ENDS_WITH:
                if (subAttribute == null) {
                    //attribute=*value
                    //attribute LIKE "%value"
                    subfilter = Filter.createSubstringFilter(attribute, null, null, value).multiValued(multivalued);
                } else {
                    //attribute=*"subAttribute":"*value"*
                    //attribute LIKE "%\"subAttribute\":\"%value\"%"
                    String sub = String.format("\"%s\":\"", subAttribute);
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub, value + "\"" }, null)
                    				.multiValued(multivalued);
                }
                break;
            case GREATER_THAN:
                if (subAttribute == null) {
                    //&(!(attribute=value))(attribute>=value)  --> LDAP does not support greater than operator
                    //attribute>"value"
                    subfilter = Filter.createGreaterOrEqualFilter(attribute, value).multiValued(multivalued);
                    subfilter = Filter.createANDFilter(
                            Filter.createNOTFilter(Filter.createEqualityFilter(attribute, value).multiValued(multivalued)),
                            subfilter
                    );
                }
                break;
            case GREATER_THAN_OR_EQUAL:
                if (subAttribute == null) {
                    //attribute>=value
                    //attribute >= "value"
                    subfilter = Filter.createGreaterOrEqualFilter(attribute, value).multiValued(multivalued);
                }
                break;
            case LESS_THAN:
                if (subAttribute == null) {
                    //&(!(attribute=value))(attribute<=value) --> LDAP does not support less than operator
                    //attribute < "value"
                    subfilter = Filter.createLessOrEqualFilter(attribute, value).multiValued(multivalued);
                    subfilter = Filter.createANDFilter(
                            Filter.createNOTFilter(Filter.createEqualityFilter(attribute, value).multiValued(multivalued)),
                            subfilter
                    );
                }
                break;
            case LESS_THAN_OR_EQUAL:
                //attribute<=value
                //attribute <= "value"
                if (subAttribute == null) {
                    subfilter = Filter.createLessOrEqualFilter(attribute, value).multiValued(multivalued);
                }
                break;
        }
        return subfilter;

    }

    private Filter getSubFilterNumeric(String subAttribute, String attribute, String value, ScimOperator operator, Type attrType, Boolean multivalued) {

        Filter subfilter = null;
        Object objValue;
        log.trace("getSubFilterNumeric {}", ldapBackend);

        //Use an integer if value depicts an integer, or even if it is not but we are on LDAP
        Double doubleValue = Double.valueOf(value);
        Integer integerPart = doubleValue.intValue();
        if (ldapBackend || (doubleValue.equals(integerPart * 1.0))) {
            objValue = integerPart;
        } else {
            objValue = doubleValue;
        }

        switch (operator) {
            case EQUAL:
            case NOT_EQUAL:
                if (subAttribute == null) {
                    //attribute=value
                    subfilter = Filter.createEqualityFilter(attribute, objValue).multiValued(multivalued);
                } else {
                    //attribute=*"subAttribute":value*
                    //attribute LIKE "%\"subAttribute\":value%"
                    String sub = String.format("\"%s\":%s", subAttribute, value);
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub }, null)
                    				.multiValued(multivalued);
                }

                subfilter = negateIf(subfilter, operator.equals(ScimOperator.NOT_EQUAL));
                break;
            case GREATER_THAN:
                if (subAttribute == null) {
                    //&(!(attribute=value))(attribute>=value)  --> LDAP does not support greater than operator
                    //attribute > value
                    subfilter = Filter.createANDFilter(
                            Filter.createNOTFilter(Filter.createEqualityFilter(attribute, objValue).multiValued(multivalued)),
                            Filter.createGreaterOrEqualFilter(attribute, objValue).multiValued(multivalued)
                    );
                }
                break;
            case GREATER_THAN_OR_EQUAL:
                if (subAttribute == null) {
                    //attribute>=value
                    subfilter = Filter.createGreaterOrEqualFilter(attribute, objValue).multiValued(multivalued);
                }
                break;
            case LESS_THAN:
                if (subAttribute == null) {
                    //&(!(attribute=value))(attribute<=value)  --> LDAP does not support less than operator
                    //attribute < value
                    subfilter = Filter.createANDFilter(
                            Filter.createNOTFilter(Filter.createEqualityFilter(attribute, objValue).multiValued(multivalued)),
                            Filter.createLessOrEqualFilter(attribute, objValue).multiValued(multivalued)
                    );
                }
                break;
            case LESS_THAN_OR_EQUAL:
                if (subAttribute == null) {
                    //attribute<=value
                    subfilter = Filter.createLessOrEqualFilter(attribute, objValue).multiValued(multivalued);
                }
                break;
            default:
                error = FilterUtil.getOperatorInconsistencyError(operator.getValue(), attrType.toString(), attribute);
        }
        return subfilter;

    }

    private Filter getSubFilterBoolean(String subAttribute, String attribute, String value, ScimOperator operator, Boolean multivalued) {

        Filter subfilter = null;
        log.trace("getSubFilterBoolean");

        if (operator.equals(ScimOperator.EQUAL) || operator.equals(ScimOperator.NOT_EQUAL)) {
            if (subAttribute == null) {
                //attribute=value
                subfilter = Filter.createEqualityFilter(attribute, Boolean.valueOf(value)).multiValued(multivalued);
            } else {
                //attribute=*"subAttribute":value*
                //attribute LIKE "%\"subAttribute\":value%"
                String sub = String.format("\"%s\":%s", subAttribute, value);
                subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub }, null)
                				.multiValued(multivalued);
            }
            subfilter = negateIf(subfilter, operator.equals(ScimOperator.NOT_EQUAL));

        } else {
            error = FilterUtil.getOperatorInconsistencyError(operator.getValue(), Type.BOOLEAN.toString(), attribute);
        }
        return subfilter;

    }

    private Filter getSubFilterDateTime(String subAttribute, String attribute, String value, ScimOperator operator, Boolean multivalued) {

        Filter subfilter = null;
        log.trace("getSubFilterDateTime");
        String stringDate = ldapBackend ? DateUtil.ISOToGeneralizedStringDate(value) : DateUtil.gluuCouchbaseISODate(value);

        if (stringDate == null) {
            error = String.format("Value passed for date comparison '%s' is not in ISO format", value);
            return null;
        }

        switch (operator) {
            case EQUAL:
            case NOT_EQUAL:
                if (subAttribute == null) {
                    //attribute=stringDate
                    //attribute="stringDate"
                    subfilter = Filter.createEqualityFilter(attribute, stringDate).multiValued(multivalued);
                } else {
                    //attribute=*"subAttribute":stringDate*
                    //attribute LIKE "%\"subAttribute\":\"stringDate\"%"
                    String sub = String.format("\"%s\":%s", subAttribute, stringDate);
                    subfilter = Filter.createSubstringFilter(attribute, null, new String[]{ sub }, null).multiValued(multivalued);
                }

                subfilter = negateIf(subfilter, operator.equals(ScimOperator.NOT_EQUAL));
                break;
            case GREATER_THAN:
                if (subAttribute == null) {
                    //&(!(attribute=value))(attribute>=value)  --> LDAP does not support greater than operator
                    //attribute > "value"
                    subfilter = Filter.createANDFilter(
                            Filter.createNOTFilter(Filter.createEqualityFilter(attribute, stringDate).multiValued(multivalued)),
                            Filter.createGreaterOrEqualFilter(attribute, stringDate).multiValued(multivalued)
                    );
                }
                break;
            case GREATER_THAN_OR_EQUAL:
                if (subAttribute == null) {
                    //attribute>=value
                    //attribute >= "value"
                    subfilter = Filter.createGreaterOrEqualFilter(attribute, stringDate).multiValued(multivalued);
                }
                break;
            case LESS_THAN:
                if (subAttribute == null) {
                    //&(!(attribute=value))(attribute<=value)  --> LDAP does not support less than operator
                    //attribute < "value"
                    subfilter = Filter.createANDFilter(
                            Filter.createNOTFilter(Filter.createEqualityFilter(attribute, stringDate).multiValued(multivalued)),
                            Filter.createLessOrEqualFilter(attribute, stringDate).multiValued(multivalued)
                    );
                }
                break;
            case LESS_THAN_OR_EQUAL:
                if (subAttribute == null) {
                    //attribute<=value
                    //attribute <= "value"
                    subfilter = Filter.createLessOrEqualFilter(attribute, stringDate).multiValued(multivalued);
                }
                break;
            default:
                error = FilterUtil.getOperatorInconsistencyError(operator.getValue(), Type.DATETIME.toString(), attribute);
        }

        return subfilter;

    }

    private Filter negateIf(Filter f, boolean negate) {
        return negate ? Filter.createNOTFilter(f) : f;
    }

}
