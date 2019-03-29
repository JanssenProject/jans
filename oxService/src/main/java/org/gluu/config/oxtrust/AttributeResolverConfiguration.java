/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import java.util.List;

import javax.enterprise.inject.Vetoed;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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
