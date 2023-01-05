/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model.configuration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Attributes
 *
 * @author Reda Zerrad Date: 07.26.2012
 * @author Yuriy Movchan Date: 08.27.2012
 */
@JsonPropertyOrder({"name", "values"})
@XmlType(propOrder = {"name", "values"})
public class CustomProperty {

    private String name;
    private List<String> values;

    public CustomProperty() {
        name = "";
        values = new ArrayList<String>();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    public List<String> getValues() {
        return this.values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

}
