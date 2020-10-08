/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;
import io.jans.as.model.jwk.JSONWebKeySet;

/**
 * @author Yuriy Movchan
 * @version 03/15/2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebKeysConfiguration extends JSONWebKeySet implements Configuration {


}
