/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.form;

import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;

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
    private transient InputStream  metaDataFile;

    
  
    
}
