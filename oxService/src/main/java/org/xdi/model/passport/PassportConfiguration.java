/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.passport;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.xdi.model.SimpleExtendedCustomProperty;

import java.util.List;

/**
 * @author Shekhar L.
 * @Date 07/17/2016
 */
@JsonPropertyOrder({"strategy", "fieldset"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportConfiguration {
    private String strategy;
    private List<SimpleExtendedCustomProperty> fieldset;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<SimpleExtendedCustomProperty> getFieldset() {
        return fieldset;
    }

    public void setFieldset(List<SimpleExtendedCustomProperty> fieldset) {
        this.fieldset = fieldset;
    }

}
