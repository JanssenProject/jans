/*
] * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.form;

import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;


import java.io.InputStream;

import org.jboss.resteasy.annotations.providers.multipart.PartType;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

public class TrustRelationshipForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
	@Valid
	@FormParam("trustRelationship")
	@PartType(MediaType.APPLICATION_JSON)
    private TrustRelationship trustRelationship;

    //Metadata FILE    
	@FormParam("metaDataFile")
	@PartType(MediaType.APPLICATION_OCTET_STREAM)
    @Schema(implementation = String.class, format="binary")
    private InputStream  metaDataFile;
    
  
	//Metadata MANUAL 
    @FormParam("metaDataContent")
    @PartType(MediaType.TEXT_PLAIN)
    @Schema(implementation = String.class, format="text")
    private String metaDataContent;
    
    //Metadata FILE 
    @FormParam("metaDataURL")
    @PartType(MediaType.APPLICATION_JSON)
    @Schema(implementation = String.class)
    private String spMetaDataURL;

    public TrustRelationship getTrustRelationship() {
        return trustRelationship;
    }

    public void setTrustRelationship(TrustRelationship trustRelationship) {
        this.trustRelationship = trustRelationship;
    }

    public InputStream getMetaDataFile() {
        return metaDataFile;
    }

    public void setMetaDataFile(InputStream metaDataFile) {
        this.metaDataFile = metaDataFile;
    }

    @Override
    public String toString() {
        return "TrustRelationshipForm ["
                + "trustRelationship=" + trustRelationship
                + "metaDataFile=" + metaDataFile 
                + "]";
    }

  
    
}
