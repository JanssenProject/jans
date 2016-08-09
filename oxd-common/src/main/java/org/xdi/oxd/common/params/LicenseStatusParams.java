/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/11/2014
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseStatusParams implements IParams {

    @JsonProperty(value = "features")
    private String features;

    public LicenseStatusParams() {
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }
}
