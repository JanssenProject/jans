/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.model.error.ErrorMessage;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Base interface for all Jans Auth configurations
 *
 * @author Yuriy Movchan Date: 12/18/2023
 */
@XmlRootElement(name = "errors")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorMessages implements Configuration {

    @XmlElementWrapper(name = "common")
    @XmlElement(name = "error")
    private List<ErrorMessage> common;

    public List<ErrorMessage> getCommon() {
        return common;
    }

    public void setCommon(List<ErrorMessage> common) {
        this.common = common;
    }

}
