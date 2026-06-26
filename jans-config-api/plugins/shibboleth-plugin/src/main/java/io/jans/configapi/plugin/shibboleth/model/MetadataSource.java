/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata source details
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataSource implements Serializable{

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "Metadata Source Type", implementation = MetadataSourceType.class )
    private MetadataSourceType metadataSourceType = MetadataSourceType.NONE; //default
    
    @Schema(description = "Metadata Source Details")
    private String metadataStr;
   
    public MetadataSourceType getMetadataSourceType() {
        return metadataSourceType;
    }
   
    public void setMetadataSourceType(MetadataSourceType metadataSourceType) {
        this.metadataSourceType = metadataSourceType;
    }
   
    public String getMetadataStr() {
        return metadataStr;
    }
    
    public void setMetadataStr(String metadataStr) {
        this.metadataStr = metadataStr;
    }
    
    
    @Override
    public String toString() {
        return "MetadataSource [metadataSourceType=" + metadataSourceType + ", metadataStr=" + metadataStr + "]";
    }
   
    
    
}
