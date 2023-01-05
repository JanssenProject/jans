/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9 February 12, 2015
 */
@XmlRootElement(name = "static")
@XmlAccessorType(XmlAccessType.FIELD)
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
