/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.configuration.Configuration;
import org.gluu.oxauth.model.jwk.JSONWebKey;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;

import javax.enterprise.inject.Vetoed;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yuriy Movchan
 * @version 03/15/2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class WebKeysConfiguration extends JSONWebKeySet implements Configuration {

    /**
     * Configuration needed to get configuration done in LDAP about algorithms supported.
     */
    private AppConfiguration appConfiguration;

    @Override
    public List<JSONWebKey> getKeys() {
        return filterKeys(super.getKeys());
    }

    /**
     * Method responsible to filter all keys stored in LDAP with the list of
     * algorithms that it is in Json config attribute called "jwksAlgorithmsSupported"
     * @param allKeys All keys that are stored in LDAP
     * @return Filtered list
     */
    private List<JSONWebKey> filterKeys(List<JSONWebKey> allKeys) {
        List<String> jwksAlgorithmsSupported = appConfiguration.getJwksAlgorithmsSupported();
        if (allKeys == null || allKeys.size() == 0
                || jwksAlgorithmsSupported == null || jwksAlgorithmsSupported.size() == 0) {
            return allKeys;
        }
        return allKeys.stream().filter(
                (key) -> jwksAlgorithmsSupported.contains(key.getAlg().getParamName())
        ).collect(Collectors.toList());
    }

    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }

}
