/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.passport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.gluu.model.passport.config.Configuration;
import org.gluu.model.passport.idpinitiated.IIConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jgomer on 2019-02-21.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportConfiguration {
	private Configuration conf;
	private IIConfiguration idpInitiated;
	private List<Provider> providers = new ArrayList<>();

	public Configuration getConf() {
		return conf;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public IIConfiguration getIdpInitiated() {
		return idpInitiated;
	}

	public void setIdpInitiated(IIConfiguration idpInitiated) {
		this.idpInitiated = idpInitiated;
	}

	public List<Provider> getProviders() {
		return providers;
	}

	public void setProviders(List<Provider> providers) {
		this.providers = providers;
	}
}
