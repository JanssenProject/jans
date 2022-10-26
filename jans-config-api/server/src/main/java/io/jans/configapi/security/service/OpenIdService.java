/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.util.AuthUtil;

import java.io.Serializable;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

@ApplicationScoped
@Named("openIdService")
public class OpenIdService implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    transient Logger log;

    @Inject
    transient AuthUtil authUtil;

    @Inject
    transient ConfigurationService configurationService;

    private transient IntrospectionService introspectionService;

    public IntrospectionService getIntrospectionService() {
        return introspectionService;
    }

    public IntrospectionResponse getIntrospectionResponse(String header, String token, String issuer) throws JsonProcessingException {
        log.debug("oAuth Introspection request , header:{}, token:{}, issuer:{}", header, token, issuer);

        String introspectionUrl = authUtil.getIntrospectionEndpoint();
        if (StringUtils.isNotBlank(issuer)) {
            introspectionUrl = AuthClientFactory.getIntrospectionEndpoint(issuer);
            log.trace("oAuth Issuer's introspectionUrl:{}", introspectionUrl);
        }

        log.info("oAuth Final introspectionUrl:{} ", introspectionUrl);
        return AuthClientFactory.getIntrospectionResponse(introspectionUrl, header, token, false);
    }

    public String requestAccessToken(final String clientId, final List<String> scope) {
        String accessToken = authUtil.requestAccessToken(clientId, scope);
        log.info("oAuth AccessToken response - accessToken:{}", accessToken);      
        return accessToken;
    }
}
