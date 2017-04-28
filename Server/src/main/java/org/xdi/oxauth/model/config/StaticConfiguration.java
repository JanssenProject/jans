/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.xdi.oxauth.model.configuration.Configuration;

import javax.enterprise.inject.Vetoed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9 February 12, 2015
 */
@XmlRootElement(name = "static")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class StaticConfiguration implements Configuration {

    @XmlElement(name = "base-dn")
    private BaseDnConfiguration baseDn;

    public BaseDnConfiguration getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(BaseDnConfiguration p_baseDn) {
        baseDn = p_baseDn;
    }
}
