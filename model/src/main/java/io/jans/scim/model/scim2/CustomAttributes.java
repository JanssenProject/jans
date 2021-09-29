/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import org.apache.commons.lang.StringUtils;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.model.scim2.util.IntrospectUtil;

import javax.lang.model.type.NullType;
import java.util.*;

/**
 * A class used to store the values of custom attributes associated to a resource extension. This class is mainly targeted
 * at users of Java <a href="https://github.com/GluuFederation/SCIM-Client">SCIM-Client</a> in order to specify/retrieve
 * custom attributes.
 * <p>Use the <code>setAttribute</code> methods to associate one (or several) values to an attribute, and use the
 * <code>getValue</code>/<code>getValues</code> methods to retrive those values when coming directly from service
 * invocations.</p>
 * <p>For both kind of operations (set/get) the data types used should ideally be consistent with the previously
 * configured custom attributes at Gluu Server. This allows your custom data to be successfully validated when sent to
 * server, or to be correctly retrieved when reading a response.</p>
 * <p>For <code>photo</code>
 * or <code>text</code>, you may use {@link String}, for <code>numeric</code> sensible choices are {@link Integer} or
 * {@link Double}, for <code>boolean</code> use {@link Boolean}, and for <code>date</code> you may use {@link Date}.</p>
 * <p>See also: {@link BaseScimResource#addCustomAttributes(CustomAttributes)} and {@link BaseScimResource#getCustomAttributes(String)}.</p>
 */
public class CustomAttributes {

    private Map<String, Object> attributeMap;
    private String uri;

    /**
     * Constructs an instance of this class to store the attribute values associated to an extension whose URI is
     * supplied.
     *
     * @param uri A string denoting the URI of an existing extension
     */
    public CustomAttributes(String uri) {
        this.uri = uri;
        attributeMap = new HashMap<>();
    }

    CustomAttributes(String uri, Map<String, Object> attributeMap) {
        this.uri = uri;
        this.attributeMap = attributeMap;

    }

    private void set(String name, Object value) {
        if (StringUtils.isNotEmpty(name) && value != null) {
            attributeMap.put(name, value);
        }
    }

    Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    /**
     * Retrieves the URI to which this <code>CustomAttributes</code> instance is tied.
     *
     * @return The URI as a String
     */
    public String getUri() {
        return uri;
    }

    /**
     * Returns the names of all attributes currently part of this instance object.
     *
     * @return A Set of Strings
     */
    public Set<String> getAttributeNames() {
        return attributeMap.keySet();
    }

    /**
     * Removes an attribute (and associated value) from this object
     *
     * @param name The name of the attribute to remove. Use the exactly the same String as when the attribute was set
     */
    public void remove(String name) {
        attributeMap.remove(name);
    }

    /**
     * Sets the value of an attribute using a <code>String</code>.
     *
     * @param name  Name of attribute
     * @param value A String object. Must be non-null, or else it won't be stored
     */
    public void setAttribute(String name, String value) {
        set(name, value);
    }

    /**
     * Sets the value of an attribute using a <code>Boolean</code>.
     *
     * @param name  Name of attribute
     * @param value A Boolean object. Must be non-null, or else it won't be stored
     */
    public void setAttribute(String name, Boolean value) {
        set(name, value);
    }

    /**
     * Sets the value of an attribute using a <code>Double</code>.
     *
     * @param name  Name of attribute
     * @param value A Double object. Must be non-null, or else it won't be stored
     */
    public void setAttribute(String name, Double value) {
        set(name, value);
    }

    /**
     * Sets the value of an attribute using an <code>Integer</code>.
     *
     * @param name  Name of attribute
     * @param value An Integer object. Must be non-null, or else it won't be stored
     */
    public void setAttribute(String name, Integer value) {
        set(name, value);
    }

    /**
     * Sets the value of an attribute using a <code>java.util.Date</code>.
     *
     * @param name  Name of attribute
     * @param value A Date object. Must be non-null, or else it won't be stored
     */
    public void setAttribute(String name, Date value) {
        if (value != null) {
            setAttribute(name, DateUtil.millisToISOString(value.getTime()));
        }
    }

    /**
     * Sets the value of an attribute using a <code>List</code> of objects.
     *
     * @param name   Name of attribute
     * @param values A non-empty, non-null list of values.
     */
    public void setAttribute(String name, List<?> values) {
        if (values != null && values.size() > 0) {
            set(name, values);
        }
    }

    /**
     * Returns the values of a multi-valued custom attribute as a <code>List</code> of objects of the specified type. If
     * you are not sure about the data type of the attribute, use <code>String</code> for type parameter T: in this case
     * {@link Object#toString() toString} is used to generate a representation.
     *
     * @param name The name of the custom attribute
     * @param cls  Specifies the type utilized to read the attribute values
     * @param <T>  Type parameter for cls
     * @return null if the attribute is not known to this object, or if it was not possible to read any of its values
     * using the type provided. Otherwise, a list of values associated to the attribute is returned (when the attribute
     * is not multivalued, a singleton list is returned: use {@link #getValue(String, Class) getValue} to get atomic
     * values instead).
     */
    public <T> List<T> getValues(String name, Class<T> cls) {

        Object val = attributeMap.get(name);
        List<T> list = null;

        if (val != null) {

            if (IntrospectUtil.isCollection(val.getClass())) {
                list = new ArrayList<>();

                for (Object item : (Collection) val) {
                    T aValue = getTypedValue(item, cls);

                    if (aValue == null) {
                        list = null;
                        break;
                    } else
                        list.add(aValue);
                }
                if (list != null && list.size() == 0)
                    list = null;
            } else {  //Create a list with one element
                T aValue = getTypedValue(val, cls);
                list = aValue == null ? null : Collections.singletonList(aValue);
            }
        }
        return list;

    }

    /**
     * Returns the value of a custom attribute as an instance of the specified type. If you are not sure about the data
     * type of the attribute, use <code>String</code> for type parameter T: in this case {@link Object#toString() toString}
     * is used to generate a representation.
     *
     * @param name The name of the custom attribute
     * @param cls  Specifies the type utilized to read the attribute value
     * @param <T>  Type parameter for cls
     * @return null if the attribute is not known to this object, or if it was not possible to read its value using the
     * type provided. Otherwise, the value associated to the attribute is returned (when the attribute is multivalued,
     * only the first value found is taken into account: use {@link #getValues(String, Class) getValues} to get all
     * values).
     */
    public <T> T getValue(String name, Class<T> cls) {

        if (!IntrospectUtil.isCollection(cls)) {
            Object val = attributeMap.get(name);
            return getTypedValue(val, cls);
        } else {
            return null;
        }

    }

    private static <T> T getTypedValue(Object val, Class<T> cls) {

        if (val != null) {
            Class classOfVal = val.getClass();

            if (IntrospectUtil.isCollection(classOfVal)) {
                //Return only the 1st
                Collection coll = (Collection) val;
                Iterator<?> iterator = coll.iterator();
                if (iterator.hasNext()) {
                    val = iterator.next();
                    classOfVal = val.getClass();
                } else {  //it's an empty collection
                    val = null;
                    classOfVal = NullType.class;
                }
            }

            if (val != null) {
                if (Date.class.equals(cls)) {
                    try {
                        val = new Date(DateUtil.ISOToMillis(val.toString()));
                    } catch (Exception e) {
                        val = null;
                    }
                } else if (String.class.equals(cls)) {
                    val = val.toString();
                } else if (!classOfVal.equals(cls)) {
                    val = null;
                }
            }
        }

        try {
            return cls.cast(val);
        } catch (Exception e) {
            return null;
        }

    }

}
