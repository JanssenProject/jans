package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Subject Token Type
 *
 * @author Yuriy Z
 */
public enum SubjectTokenType {

    ID_TOKEN("urn:ietf:params:oauth:token-type:id_token"),
    ACCESS_TOKEN("urn:ietf:params:oauth:token-type:access_token");

    private final String name;

    SubjectTokenType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the corresponding {@link ExchangeTokenType} for a parameter token type.
     *
     * @param param The token type parameter.
     * @return The corresponding token type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static SubjectTokenType fromString(String param) {
        if (param != null) {
            for (SubjectTokenType rt : SubjectTokenType.values()) {
                if (param.equalsIgnoreCase(rt.name)) {
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the token type parameter.
     */
    @Override
    @JsonValue
    public String toString() {
        return name;
    }
    }
