/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import io.jans.orm.annotation.AttributeEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * This class allows to enumerate and identify the possible values of the
 * parameter response_type for the authorization endpoint.
 * </p>
 * <p>
 * The client informs the authorization server of the desired grant type.
 * </p>
 * <p>
 * The authorization endpoint is used by the authorization code grant type and
 * implicit grant type flows.
 * </p>
 *
 * @author Javier Rojas Blum
 * @version July 18, 2017
 */
public enum ResponseType implements HasParamName, AttributeEnum {

    /**
     * Used for the authorization code grant type.
     */
    @JsonProperty("code")
    CODE("code", "Authorization Code Grant Type"),
    /**
     * Used for the implicit grant type.
     */
    @JsonProperty("token")
    TOKEN("token", "Implicit Grant Type"),
    /**
     * Include an ID Token in the authorization response.
     */
    @JsonProperty("id_token")
    ID_TOKEN("id_token", "ID Token");

    private final String value;
    private final String displayName;

    private static final Map<String, ResponseType> mapByValues = new HashMap<>();

    static {
        for (ResponseType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    ResponseType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    /**
     * Returns the corresponding {@link ResponseType} for a single parameter response_type.
     *
     * @param param The response_type parameter.
     * @return The corresponding response type if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static ResponseType fromString(String param) {
        if (param != null) {
            for (ResponseType rt : ResponseType.values()) {
                if (param.equals(rt.value)) {
                    return rt;
                }
            }
        }

        return null;
    }

    /**
     * Gets param name.
     *
     * @return param name
     */
    public String getParamName() {
        return value;
    }

    /**
     * Gets display name
     *
     * @return display name name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Returns a list of the corresponding {@link ResponseType} from a space-separated
     * list of response_type parameters.
     *
     * @param paramList A space-separated list of response_type parameters.
     * @param separator The separator of the string list.
     * @return A list of the recognized response types.
     */
    public static List<ResponseType> fromString(String paramList, String separator) {
        List<ResponseType> responseTypes = new ArrayList<>();

        if (paramList != null && !paramList.isEmpty()) {
            String[] params = paramList.split(separator);
            for (String param : params) {
                for (ResponseType rt : ResponseType.values()) {
                    if (param.equals(rt.value) && !responseTypes.contains(rt)) {
                        responseTypes.add(rt);
                    }
                }
            }
        }

        return responseTypes;
    }

    public static boolean isImplicitFlow(String responseTypes) {
        if (StringUtils.isBlank(responseTypes)) {
            return false;
        }
        return !responseTypes.contains("code") && (responseTypes.contains("id_token") || responseTypes.contains("token"));
    }

    public static List<String> toStringList(List<ResponseType> responseTypes) {
        if (responseTypes == null) {
            return Lists.newArrayList();
        }
        return responseTypes.stream().map(ResponseType::getValue).collect(Collectors.toList());
    }

    public static String[] toStringArray(ResponseType[] responseTypes) {
        if (responseTypes == null) {
            return new String[0];
        }

        String[] resultResponseTypes = new String[responseTypes.length];
        for (int i = 0; i < responseTypes.length; i++) {
            resultResponseTypes[i] = responseTypes[i].getValue();
        }

        return resultResponseTypes;
    }

    public static ResponseType getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the response_type parameter.
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }

}