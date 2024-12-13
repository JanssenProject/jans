/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Supported MetadataServer
 *
 * @author Shekhar L. on 06/08/2024
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataServer {

    private String url;

	private List<String> certificateDocumentInum = new ArrayList<String>();;


	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}


	public List<String> getCertificateDocumentInum() {
		return certificateDocumentInum;
	}

	public void setCertificateDocumentInum(List<String> certificateDocumentInum) {
		this.certificateDocumentInum = certificateDocumentInum;
	}
}
