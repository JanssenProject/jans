/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.passport;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.xdi.model.passport.config.Configuration;
import org.xdi.model.passport.idpinitiated.IIConfiguration;

import java.util.List;

/**
 * @author Shekhar L.
 * @Date 07/17/2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportConfiguration {

    private Configuration conf;
    private IIConfiguration idpInitiated;
    private List<ProviderDetails> providers;

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

    public List<ProviderDetails> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderDetails> providers) {
        this.providers = providers;
    }

}
