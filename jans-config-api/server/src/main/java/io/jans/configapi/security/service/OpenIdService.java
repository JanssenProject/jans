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
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
@Named("openIdService")
public class OpenIdService implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Inject
    ConfigurationService configurationService;

    private IntrospectionService introspectionService;

    public IntrospectionService getIntrospectionService() {
        return introspectionService;
    }

    public String getIntrospectionEndpoint() {
        return configurationService.find().getIntrospectionEndpoint();
    }

    public String getTokenEndpoint() {
        return configurationService.find().getTokenEndpoint();
    }

    public IntrospectionResponse getIntrospectionResponse(String header, String token, String issuer) throws Exception {
        log.debug("oAuth Introspection request , header:{}, token:{}, issuer:{}", header, token, issuer);

        String introspectionUrl = getIntrospectionEndpoint();
        if (StringUtils.isNotBlank(issuer)) {
            introspectionUrl = AuthClientFactory.getIntrospectionEndpoint(issuer);
            log.trace("\n\n oAuth Issuer's introspectionUrl = " + introspectionUrl);
        }

        log.info("\n\n oAuth Final introspectionUrl = " + introspectionUrl);
        return AuthClientFactory.getIntrospectionResponse(introspectionUrl, header, token, false);
    }

    public String requestAccessToken(final String clientId, final List<String> scope) throws Exception {
        log.info("oAuth request AccessToken - clientId:{}, scope:{} ", clientId, scope);
        String tokenUrl = getTokenEndpoint();
        Token token = authUtil.requestAccessToken(tokenUrl, clientId, scope);
        log.info("oAuth AccessToken response - token = " + token);
        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }
}
