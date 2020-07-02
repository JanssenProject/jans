package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gluu.oxd.common.Jackson2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StringParam implements IParams {
    private static final Logger LOG = LoggerFactory.getLogger(StringParam.class);
    @JsonProperty(value = "value")
    String value;

    public StringParam() {
    }

    public StringParam(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "StringParam{" +
                "value='" + value + '\'' +
                '}';
    }

    public String toJsonString() {
        try {
            return Jackson2.serializeWithoutNulls(this);
        } catch (IOException e) {
            LOG.error("Error in parsing StringParam object.", e);
            throw new RuntimeException(e);
        }
    }
}
