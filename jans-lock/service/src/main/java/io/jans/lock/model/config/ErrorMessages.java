/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.model.error.ErrorMessage;
import jakarta.enterprise.inject.Vetoed;

/**
 * Base interface for all Jans Auth configurations
 *
 * @author Yuriy Movchan Date: 12/18/2023
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorMessages implements Configuration {

    private List<ErrorMessage> common;

    public List<ErrorMessage> getCommon() {
        return common;
    }

    public void setCommon(List<ErrorMessage> common) {
        this.common = common;
    }

}
