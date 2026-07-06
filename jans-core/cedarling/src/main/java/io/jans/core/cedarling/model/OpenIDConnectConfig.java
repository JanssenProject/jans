/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.core.cedarling.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


@JsonIgnoreProperties(ignoreUnknown = true)
@XmlAccessorType(XmlAccessType.FIELD)
public class OpenIDConnectConfig {

	@DocProperty(description = "Trusted identity provider")
    @Schema(description = "the trusted identity provider")
    private String issuer;

	

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	@Override
	public String toString() {
		return "OpenIDConnectConfig [issuer=" + issuer+ "]";
	}

}
