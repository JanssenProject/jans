/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.enterprise.inject.Vetoed;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticConfiguration implements Configuration {

	@XmlElement(name = "base-dn")
	private BaseDnConfiguration baseDn;

	public BaseDnConfiguration getBaseDn() {
		return baseDn;
	}

	public void setBaseDn(BaseDnConfiguration baseDn) {
		this.baseDn = baseDn;
	}
}
