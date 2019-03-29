/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public final class Util {

    public static final String UTF8 = "UTF-8";

    private Util() { }

    /**
     * Converts an array to the appropriate ArrayList
     *
     * @param <T>
     *            The type held in the array
     * @param array
     *            The array of values
     * @return An ArrayList<T> of the same values *
     */
    public static <T> ArrayList<T> array2ArrayList(final T[] array) {
        if ((array == null) || (array.length == 0)) {
            return new ArrayList<T>();
        }
        final List<T> list = Arrays.asList(array);
        return new ArrayList<T>(list);
    }

    /**
     * Convert a list of unknown type to a List<String> in a type-safe way
     *
     * @param list
     *            List to cast/convert
     * @return List<String> of the items in list
     */
    public static List<String> makeStringList(final List<?> list) {
        final List<String> returnList = new ArrayList<String>();
        for (final Object o : list) {
            returnList.add(o.toString().trim());
        }
        return returnList;
    }

    /**
     * Convert a single String to a List<String>
     *
     * @param item
     *            Item to return as a list
     * @return A one-item list containing the parameter
     */
    public static List<String> makeStringList(final String item) {
        final List<String> returnList = new ArrayList<String>(1);
        returnList.add(item.trim());
        return returnList;
    }

    /**
     * Convert a String array to an List<String>
     *
     * @param array
     *            String array to convert
     * @return List<String> of the items in the array
     */
    public static List<String> makeStringList(final String[] array) {
        return array2ArrayList(array);
    }

    /**
     * Return unique members of a list
     *
     * @param <E>
     *            Type of the list
     * @param list
     *            List to process
     * @return Distinct members of the list
     */
    public static <E> List<E> getDistinct(final List<E> list) {
        final Set<E> set = new LinkedHashSet<E>(list);
        return new ArrayList<E>(set);
    }

    /**
     * Json representation of object.
     *
     * @param object
     *            object to represent
     * @return json as string
     * @throws IOException
     */
    public static String asJson(Object object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(object);
    }

    /**
     * Pretty json representation of object.
     *
     * @param object
     *            object to represent
     * @return json as string
     * @throws IOException
     */
    public static String asPrettyJson(Object object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
        final ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
        return writer.writeValueAsString(object);
    }

    /**
     * Creates json mapper.
     *
     * @return json mapper
     */
    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = new AnnotationIntrospector.Pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().withAnnotationIntrospector(pair);
        mapper.getSerializationConfig().withAnnotationIntrospector(pair);
        return mapper;
    }

    /**
     * Get the First RDN
     *
     * @param dn
     *            The DN stored in another attribute
     * @return The first RDN
     */
    public static String getFirstRDN(String dn) {
        if (dn == null) {
            return null;
        }

        String rdn = dn.split(",")[0];
        return rdn;

    }

    /**
     * Use this method to find the value of first rdn Example: dn=
     * cn=11111__CallAdministrator,ou=groups,dc=radianz,dc=com will return
     * "11111__CallAdministrator" as rdn value
     *
     * @param dn
     * @return String cn
     */
    public static String getFirstRDNValue(String dn) {
        if (dn == null) {
            return null;
        }

        String rdn = dn.split(",")[0];
        return rdn.split("=")[1];

    }

    /**
     * Get the Second RDN
     *
     * @param dn
     *            The DN stored in another attribute
     * @return The first RDN
     */
    public static String getSecondRDN(String dn) {
        if (dn == null) {
            return null;
        }

        String rdn = dn.split(",")[1];
        return rdn;

    }

    /**
     * Get the Second RDN Value
     *
     * @param dn
     *            The DN stored in another attribute
     * @return The first RDN
     */
    public static String getSecondRDNValue(String dn) {
        if (dn == null) {
            return null;
        }

        String rdn = dn.split(",")[1];
        return rdn.split("=")[1];

    }

    public static boolean equals(String str1, String str2) {
        if (str1 == null) {
            if (str2 != null) {
                return false;
            }
        } else if (!str1.equalsIgnoreCase(str2)) {
            return false;
        }

        return true;
    }

    public static boolean empty(String str) {
        return (str == null) || (str.trim().length() == 0);
    }

    public static String encodeString(String str) {
        if ((str == null) || (str.length() == 0)) {
            return str;
        }
        try {
            return (new URI(null, str, null)).toString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    public static String getEmtpyStringIfNull(String str) {
        return (str == null) ? "" : str;
    }

    public static <T> void removeDuplicateWithOrder(List<T> list) {
        Set<Object> set = new HashSet<Object>();
        List<T> newList = new ArrayList<T>();
        for (T item : list) {
            if (set.add(item)) {
                newList.add(item);
            }
        }
        list.clear();
        list.addAll(newList);
    }

}
