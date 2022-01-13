/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.antlr.scimFilter.util;

import java.math.BigDecimal;
import java.util.Map;
import java.time.Instant;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.scim2.AttributeDefinition.Type;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.extensions.ExtensionField;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.service.antlr.scimFilter.enums.CompValueType;
import io.jans.scim.service.antlr.scimFilter.enums.ScimOperator;
import io.jans.scim.service.scim2.ExtensionService;

/**
 * Created by jgomer on 2017-12-10.
 */
public class SimpleExpression {

    private Logger log = LogManager.getLogger(getClass());
    private ExtensionService extService;

    private String attribute;
    private ScimOperator operator;
    private CompValueType type;
    private String attributeValue;

    private String parentAttribute;
    private Class<? extends BaseScimResource> resourceClass;

    public SimpleExpression(String attribute, ScimOperator operator, CompValueType type, String attributeValue){
        this.attribute = attribute;
        this.operator=operator;
        this.attributeValue=attributeValue;
        this.type=type;
        //Extension service is not needed since this class deals with evaluating subatributes living inside complex attributes
        //but the server does not support complex custom attributes
        //extService= CdiUtil.bean(ExtensionService.class);
    }

    public Boolean evaluate(Map<String, Object> item){
        /*
        There are 3 categories for attribute operators:
        - eq, ne (applicable to all types)
        - co, sw, ew (applicable to STRING, REFERENCE)
        - gt, ge, lt, le (applicable to STRING, DECIMAL, REFERENCE, DATETIME)
         */
        Boolean val=null;
        Type attrType=null;

        log.trace("SimpleExpression.evaluate.");
        String msg=String.format("%s%s",
                StringUtils.isEmpty(parentAttribute) ? "" : (parentAttribute + "."), resourceClass.getSimpleName());

        Attribute attrAnnot=getAttributeAnnotation();
        if (attrAnnot==null) {
            if (extService != null) {
                ExtensionField field=extService.getFieldOfExtendedAttribute(resourceClass, attribute);

                if (field == null)
                    log.error("SimpleExpression.evaluate. Attribute '{}' is not recognized in {}", attribute, msg);
                else
                    attrType = field.getAttributeDefinitionType();
            }
        }
        else
            attrType = attrAnnot.type();

        if (attrType==null) {
            log.error("SimpleExpression.evaluate. Could not determine type of attribute '{}' in {}", attribute, msg);
        }
        else {
            String errMsg=FilterUtil.checkFilterConsistency(attribute, attrType, type, operator);

            if (errMsg==null){
                Object currentAttrValue=item.get(attribute);

                if (type.equals(CompValueType.NULL)) {   //implies attributeValue==null
                    log.trace("SimpleExpression.evaluate. Using null as compare value");
                    val=operator.equals(ScimOperator.EQUAL) ? currentAttrValue==null : currentAttrValue!=null;
                }
                else
                if (currentAttrValue==null){
                    //If value is absent, filter won't match against anything (only when comparing with null as in previous case)
                    log.trace("SimpleExpression.evaluate. Attribute \"{}\" is absent in resource data", attribute);
                    val=false;
                }
                else
                if (Type.STRING.equals(attrType) || Type.REFERENCE.equals(attrType))    //check it's a string or reference
                    val = evaluateStringAttribute(attrAnnot!=null && attrAnnot.isCaseExact(), currentAttrValue);
                else
                if (Type.INTEGER.equals(attrType) || Type.DECIMAL.equals(attrType))
                    val = evaluateNumericAttribute(attrType, currentAttrValue);
                else
                if (Type.BOOLEAN.equals(attrType))
                    val = evaluateBooleanAttribute(attrType, currentAttrValue);
                else
                if (Type.DATETIME.equals(attrType))
                    val = evaluateDateTimeAttribute(attrType, currentAttrValue);
            }
            else
                log.error("SimpleExpression.evaluate. {}", errMsg);
        }
        return val;

    }

    private Boolean evaluateDateTimeAttribute(Type attrType, Object valueInItemObj){

        Boolean val=null;
        log.trace("SimpleExpression.evaluateDateTimeAttribute");

        try{
            long valueInItem = Instant.parse(valueInItemObj.toString()).toEpochMilli();
            long compareAgainst = Instant.parse(attributeValue).toEpochMilli();

            switch (operator){
                case EQUAL:
                    val=valueInItem==compareAgainst;
                    break;
                case NOT_EQUAL:
                    val=valueInItem!=compareAgainst;
                    break;
                case GREATER_THAN:
                    val=valueInItem>compareAgainst;
                    break;
                case GREATER_THAN_OR_EQUAL:
                    val=valueInItem>=compareAgainst;
                    break;
                case LESS_THAN:
                    val=valueInItem<compareAgainst;
                    break;
                case LESS_THAN_OR_EQUAL:
                    val=valueInItem<=compareAgainst;
                    break;
                default:
                    FilterUtil.logOperatorInconsistency(operator.getValue(), attrType.toString(), attribute);
            }
        }
        catch (Exception e){
            log.error("SimpleExpression.evaluate. Date not in ISO format");
        }
        return val;

    }

    private Boolean evaluateBooleanAttribute(Type attrType, Object valueInItemObj){

        Boolean val=null;
        log.trace("SimpleExpression.evaluateBooleanAttribute");

        boolean valueInItem=Boolean.valueOf(valueInItemObj.toString());
        boolean compareAgainst=Boolean.valueOf(attributeValue);

        if (operator.equals(ScimOperator.EQUAL))
            val=valueInItem==compareAgainst;
        else
        if (operator.equals(ScimOperator.NOT_EQUAL))
            val=valueInItem!=compareAgainst;
        else
            FilterUtil.logOperatorInconsistency(operator.getValue(), attrType.toString(), attribute);

        return val;

    }

    private Boolean evaluateNumericAttribute(Type attrType, Object valueInItemObj){

        Boolean val=null;
        log.trace("SimpleExpression.evaluateNumericAttribute");

        BigDecimal valueInItem=new BigDecimal(valueInItemObj.toString());
        BigDecimal compareAgainst=new BigDecimal(attributeValue);

        switch (operator) {
            case EQUAL:
                val=valueInItem.equals(compareAgainst);
                break;
            case NOT_EQUAL:
                val=!valueInItem.equals(compareAgainst);
                break;
            case GREATER_THAN:
                val=valueInItem.compareTo(compareAgainst)>0;
                break;
            case GREATER_THAN_OR_EQUAL:
                val=valueInItem.equals(compareAgainst) || valueInItem.compareTo(compareAgainst)>0;
                break;
            case LESS_THAN:
                val=valueInItem.compareTo(compareAgainst)<0;
                break;
            case LESS_THAN_OR_EQUAL:
                val=valueInItem.equals(compareAgainst) || valueInItem.compareTo(compareAgainst)<0;
                break;
            default:
                FilterUtil.logOperatorInconsistency(operator.getValue(), attrType.toString(), attribute);
        }
        return val;

    }

    private Boolean evaluateStringAttribute(boolean caseExact, Object valueInItemObj) {

        //Here attributeValue is never null
        Boolean val=null;
        log.trace("SimpleExpression.evaluateStringAttribute");

        String valueInItem=valueInItemObj.toString();

        if (!caseExact){
            valueInItem=valueInItem.toLowerCase();
            attributeValue=attributeValue.toLowerCase();
        }
        switch (operator){
            case EQUAL:
                val=valueInItem.equals(attributeValue);
                break;
            case NOT_EQUAL:
                val=!valueInItem.equals(attributeValue);
                break;
            case CONTAINS:
                val=valueInItem.contains(attributeValue);
                break;
            case STARTS_WITH:
                val=valueInItem.startsWith(attributeValue);
                break;
            case ENDS_WITH:
                val=valueInItem.endsWith(attributeValue);
                break;
            case GREATER_THAN:
                val=valueInItem.compareTo(attributeValue)>0;
                break;
            case GREATER_THAN_OR_EQUAL:
                val=valueInItem.equals(attributeValue) || valueInItem.compareTo(attributeValue)>0;
                break;
            case LESS_THAN:
                val=valueInItem.compareTo(attributeValue)<0;
                break;
            case LESS_THAN_OR_EQUAL:
                val=valueInItem.equals(attributeValue) || valueInItem.compareTo(attributeValue)<0;
                break;
        }
        return val;

    }

    private Attribute getAttributeAnnotation(){
        String attr= StringUtils.isEmpty(parentAttribute) ? attribute : parentAttribute + "." + attribute;
        return IntrospectUtil.getFieldAnnotation(attr, resourceClass==null ? BaseScimResource.class : resourceClass, Attribute.class);
    }

    public void setParentAttribute(String parentAttribute) {
        this.parentAttribute = parentAttribute;
    }

    public void setResourceClass(Class<? extends BaseScimResource> resourceClass) {
        this.resourceClass = resourceClass;
    }

}
