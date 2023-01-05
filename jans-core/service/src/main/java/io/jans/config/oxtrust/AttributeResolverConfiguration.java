/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.enterprise.inject.Vetoed;
import java.util.List;


/**
 * Attribute resolver configurations
 *
 * @author Yuriy Movchan Date: 09/04/2017
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeResolverConfiguration implements Configuration {

    private List<NameIdConfig> nameIdConfigs;

    public List<NameIdConfig> getNameIdConfigs() {
        return nameIdConfigs;
    }

    public void setNameIdConfigs(List<NameIdConfig> nameIdConfigs) {
        this.nameIdConfigs = nameIdConfigs;
    }

}
