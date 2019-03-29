package org.gluu.oxauth.model.ldap;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class TokenAttributes implements Serializable {

    @JsonProperty("x5cs256")
    private String x5cs256;

    public String getX5cs256() {
        return x5cs256;
    }

    public void setX5cs256(String x5cs256) {
        this.x5cs256 = x5cs256;
    }

    @Override
    public String toString() {
        return "TokenAttributes{" +
                "x5cs256='" + x5cs256 + '\'' +
                '}';
    }
}
