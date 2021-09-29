/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.extensions;

import io.jans.model.attribute.AttributeDataType;
import io.jans.scim.model.scim2.AttributeDefinition.Type;
import io.jans.scim.model.scim2.util.DateUtil;

import java.util.regex.Pattern;

/**
 * Represents the metadata of an attribute that belongs to a SCIM resource extension.
 * <p>Attributes part of extensions, aka "extended attributes" or "custom attributes" have implicit characteristics equal
 * to the defaults shown in section 2.2 of RFC 7643. In other words, all custom attributes have:</p>
 * <ul>
 *     <li>canonicalValues: <i>none assigned</i></li>
 *     <li>required: <i>false</i></li>
 *     <li>caseExact: <i>false</i></li>
 *     <li>mutability: <i>readWrite</i></li>
 *     <li>returned: <i>default</i></li>
 *     <li>uniqueness: <i>none</i></li>
 *     <li>referenceTypes: <i>none</i></li>
 * </ul>
 * <p>The only "configurable" characteristics supported are:</p>
 * <ul>
 *     <li>multivalued: true/false</li>
 *     <li>type: reference, string, datetime, decimal, and boolean (only).</li>
 * </ul>
 *
 * <p>See also: {@link Extension} class.</p>
 */
/*
 * Created by jgomer on 2017-09-29.
 */
public class ExtensionField {

    private static final String XSD_DATE_TIME_PATTERN="^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*$";

    private String name;
    private boolean multiValued;
    private AttributeDataType type;
    private String description;

    /**
     * Tries parsing the value passed according to the data type associated to the field
     * @param field An ExtensionField instance that determines the data type expected to be received
     * @param val A non-null object that represents a (hopefully valid) value for this field
     * @return Null if the value is not consistent with the data type expected. Otherwise, the same value received is returned
     */
    public static Object valueOf(ExtensionField field, Object val){

        Object value=null;
        switch (field.getType()){
            case STRING:
            case BINARY:
                if (val instanceof String)
                    value = val;
                break;
            case DATE:
                //Dates are stored and read as strings indeed (no usage of Date-related classes take place)
                if (val instanceof String) {
                    Pattern p = Pattern.compile(XSD_DATE_TIME_PATTERN);
                    if (p.matcher(val.toString()).find())
                        value=val;
                }
                break;
            case NUMERIC:
                if (val instanceof Integer || val instanceof Double)
                    value = val;
                break;
            case BOOLEAN:
                if (val instanceof Boolean)
                    value = val;
                break;
        }
        return value;

    }

    /**
     * Equivalent to {@link #valueOf(ExtensionField, Object) valueOf}, however, a String is supplied as value. Here no
     * validations on data type consistence takes place (it is expected that value passed reflects the type of the field).
     * If the field is a DATE, a conversion from ISO format is done, nonetheless, DATE fields still remain being represented
     * as Java strings
     * @param field An ExtensionField
     * @param val A non-null String value
     * @return A value
     */
    public static Object valueFromString(ExtensionField field, String val){

        Object value=null;  //In practice value will never end up being null
        switch (field.getType()){
            case STRING:
            case BINARY:
                value = val;
                break;
            case DATE:
                //Dates are stored and read as strings indeed (no handling of Date or DateTime objects)
                value=DateUtil.generalizedToISOStringDate(val);
                break;
            case NUMERIC:
                try{
                    value = new Integer(val);
                } catch (Exception e) {
                    try {
                        value = new Double(val);
                    } catch (Exception e2) {
                        value = null;
                    }
                }
                break;
            case BOOLEAN:
                value=Boolean.valueOf(val);
                break;
        }
        return value;

    }

    /**
     * Takes an object and a ExtensionField, and returns a String (suitable for storing in LDAP). For a field data type
     * NUMERIC, BOOLEAN or STRING, a straight string representation is returned. When it's DATE, it is converted from ISO
     * to generalized date time format.
     * @param field An instance of ExtensionField
     * @param val A value
     * @return String formated properly
     * @deprecated
     */
    public static String stringValueOf(ExtensionField field, Object val){

        String value=null;
        switch (field.getType()) {
            case NUMERIC:
            case STRING:
            case BINARY:
                value=val.toString();
                break;
            case DATE:
                value=DateUtil.ISOToGeneralizedStringDate(val.toString());
                break;
            case BOOLEAN:
                value=val.toString();
                break;
        }
        return value;

    }

    /**
     * <p>Maps the org.gluu.model.AttributeDataType associated to this ExtensionField (see {@link #getType() getType}) to
     * a member of the enum {@link io.jans.scim.model.scim2.AttributeDefinition.Type AttributeDefinition.Type}.</p>
     * <p>The mapping is straightforward. Special case is org.gluu.model.AttributeDataType.NUMERIC mapped to
     * {@link io.jans.scim.model.scim2.AttributeDefinition.Type#DECIMAL Type.DECIMAL}.</p>
     * @return An enum value of AttributeDefinition.Type
     */
    public Type getAttributeDefinitionType(){

        Type attrType=null;
        switch (type) {
            case BINARY:
                attrType = Type.BINARY;
                break;
            case STRING:
                attrType = Type.STRING;
                break;
            case DATE:
                attrType = Type.DATETIME;
                break;
            case NUMERIC:
                attrType = Type.DECIMAL; //Return the broader of DECIMAL or INTEGER;
                break;
            case BOOLEAN:
                attrType = Type.BOOLEAN;
                break;
        }
        return attrType;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public AttributeDataType getType() {
        return type;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public void setType(AttributeDataType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}