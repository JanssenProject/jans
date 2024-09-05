/*
] * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.form;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;


import java.io.InputStream;

import org.jboss.resteasy.annotations.providers.multipart.PartType;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import io.jans.service.document.store.model.Document;
import io.swagger.v3.oas.annotations.media.Schema;

public class AssetForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
	@Valid
	@FormParam("document")
	@PartType(MediaType.APPLICATION_JSON)
    private Document document;

    @NotNull
	@FormParam("assetFile")
	@PartType(MediaType.APPLICATION_OCTET_STREAM)
    @Schema(implementation = String.class, format="binary")
    private InputStream  assetFile;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public InputStream getAssetFile() {
        return assetFile;
    }

    public void setAssetFile(InputStream assetFile) {
        this.assetFile = assetFile;
    }

    @Override
    public String toString() {
        return "AssetForm [document=" + document + ", assetFile=" + assetFile + "]";
    }

}
