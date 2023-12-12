/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.form;

import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;


import java.io.InputStream;

import org.jboss.resteasy.annotations.providers.multipart.PartType;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

public class BrokerIdentityProviderForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
	@Valid
	@FormParam("identityProvider")
	@PartType(MediaType.APPLICATION_JSON)
    private IdentityProvider identityProvider;

    @NotNull
	@FormParam("metaDataFile")
	@PartType(MediaType.APPLICATION_OCTET_STREAM)
    @Schema(implementation = String.class, format="binary")
    private InputStream  metaDataFile;

    public IdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    public InputStream getMetaDataFile() {
        return metaDataFile;
    }

    public void setMetaDataFile(InputStream metaDataFile) {
        this.metaDataFile = metaDataFile;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public String toString() {
        return "BrokerIdentityProviderForm [identityProvider=" + identityProvider + "]";
    }

  
    
}
