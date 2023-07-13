/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import io.jans.configapi.plugin.saml.model.TrustRelationship;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Valid;


import java.io.File;

import org.jboss.resteasy.annotations.providers.multipart.PartType;
import org.jboss.resteasy.annotations.providers.multipart.FormParams;

public class TrustRelationshipForm implements Serializable {



    @NotNull
	@Valid
	@FormParams("trustRelationship")
	@PartType("application/json")
    private TrustRelationship trustRelationship;

    @NotNull
	@FormParams("metaDataFile")
	@PartType("application/octet-stream")
    private File metaDataFile;

    public TrustRelationship getTrustRelationship() {
        return trustRelationship;
    }

    public void setTrustRelationship(TrustRelationship trustRelationship) {
        this.trustRelationship = trustRelationship;
    }

    public File getMetaDataFile() {
        return metaDataFile;
    }

    public void setMetaDataFile(File metaDataFile) {
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
