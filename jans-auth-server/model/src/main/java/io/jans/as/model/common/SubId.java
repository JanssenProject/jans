package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Z
 */
public class SubId {

    @JsonProperty("format")
    private String format;

    @JsonProperty("id")
    private String id;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
