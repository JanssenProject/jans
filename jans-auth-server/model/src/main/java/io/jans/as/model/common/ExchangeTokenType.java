package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.as.model.config.Constants;

/**
 * @author Yuriy Z
 */
public enum ExchangeTokenType {

    ACCESS_TOKEN(Constants.TOKEN_TYPE_ACCESS_TOKEN),
    DEVICE_SECRET(Constants.ACTOR_TOKEN_TYPE_DEVICE_SECRET),
    ID_TOKEN(Constants.SUBJECT_TOKEN_TYPE_ID_TOKEN),
    TX_TOKEN(Constants.TOKEN_TYPE_TX_TOKEN);

    private final String name;

    ExchangeTokenType(String name) {
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
    public static ExchangeTokenType fromString(String param) {
        if (param != null) {
            for (ExchangeTokenType rt : ExchangeTokenType.values()) {
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
