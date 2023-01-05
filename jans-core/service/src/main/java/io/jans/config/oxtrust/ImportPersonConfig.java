/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.enterprise.inject.Vetoed;
import java.io.Serializable;
import java.util.List;


/**
 * Janssen Project configuration
 *
 * @author shekhar laad
 * @date 12/10/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class ImportPersonConfig implements Configuration, Serializable {

    private static final long serialVersionUID = 2686538577505167695L;

    private List<ImportPerson> mappings;

    public List<ImportPerson> getMappings() {
        return mappings;
    }

    public void setMappings(List<ImportPerson> mappings) {
        this.mappings = mappings;
    }

}
