/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.config;

import javax.enterprise.inject.Vetoed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.gluu.oxauth.model.configuration.Configuration;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;

/**
 * @author Yuriy Movchan
 * @version 03/15/2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class WebKeysConfiguration extends JSONWebKeySet implements Configuration {
}
