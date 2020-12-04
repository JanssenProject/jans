/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "value1", "value2", "hide", "description" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleExtendedCustomProperty extends SimpleCustomProperty {

    private static final long serialVersionUID = 7413216569115979793L;

    @JsonProperty("hide")
    private boolean hideValue;

    public SimpleExtendedCustomProperty() {
    }

    public SimpleExtendedCustomProperty(String value1, String value2) {
        super(value1, value2);
    }

    public SimpleExtendedCustomProperty(String value1, String value2, boolean hideValue) {
        super(value1, value2);
        this.hideValue = hideValue;
    }

    public SimpleExtendedCustomProperty(String value1, String value2, String description) {
        super(value1, value2, description);
    }

    public SimpleExtendedCustomProperty(String value1, String value2, String description, boolean hideValue) {
        super(value1, value2, description);
        this.hideValue = hideValue;
    }

    public boolean getHideValue() {
        return hideValue;
    }

    public void setHideValue(boolean hideValue) {
        this.hideValue = hideValue;
    }

}
