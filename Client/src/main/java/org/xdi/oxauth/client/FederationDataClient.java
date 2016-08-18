/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xdi.oxauth.model.federation.FederationRequest;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/10/2012
 */

public class FederationDataClient extends BaseClient<FederationDataRequest, FederationDataResponse> {

    private static final Logger LOG = Logger.getLogger(FederationDataClient.class);

    public FederationDataClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public FederationDataResponse joinRP(String p_federationId, String p_displayName, String p_redirectUri) {
        return joinRP(p_federationId, p_displayName, p_redirectUri, null, null);
    }

    public FederationDataResponse joinRP(String p_federationId, String p_displayName, String p_redirectUri, String p_x509pem, String p_x509url) {
        setRequest(new FederationDataRequest());
        getRequest().setType(FederationRequest.Type.RP);
        getRequest().setFederationId(p_federationId);
        getRequest().setDisplayName(p_displayName);
        getRequest().setRedirectUri(p_redirectUri);
        getRequest().setX509pem(p_x509pem);
        getRequest().setX509url(p_x509url);
        return exec(getRequest());
    }

    public FederationDataResponse joinOP(String p_federationId, String p_displayName, String p_opId, String p_domain) {
        return joinOP(p_federationId, p_displayName, p_opId, p_domain, null, null);
    }

    public FederationDataResponse joinOP(String p_federationId, String p_displayName, String p_opId, String p_domain, String p_x509pem, String p_x509url) {
        setRequest(new FederationDataRequest());
        getRequest().setType(FederationRequest.Type.OP);
        getRequest().setFederationId(p_federationId);
        getRequest().setDisplayName(p_displayName);
        getRequest().setOpId(p_opId);
        getRequest().setDomain(p_domain);
        getRequest().setX509pem(p_x509pem);
        getRequest().setX509url(p_x509url);
        return exec(getRequest());
    }

    private FederationDataResponse exec(FederationDataRequest p_request) {
        setResponse(new FederationDataResponse());
        final String httpMethod = getHttpMethod();

        initClientRequest();
        clientRequest.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        clientRequest.setHttpMethod(httpMethod);

        try {
            if (HttpMethod.POST.equals(httpMethod)) {
                putAllFormParameters(clientRequest, p_request);

                clientResponse = clientRequest.post(String.class);

                setRequest(p_request);

                final String entity = clientResponse.getEntity(String.class);

                getResponse().setStatus(clientResponse.getStatus());
                getResponse().setHeaders(clientResponse.getHeaders());
                getResponse().setLocation(clientResponse.getLocation() != null ? clientResponse.getLocation().getHref() : "");
                getResponse().setEntity(entity);

                if (StringUtils.isNotBlank(entity)) {
                    getResponse().injectErrorIfExistSilently(entity);
                }
            } else {
                LOG.error("HTTP method is not supported. Method:" + httpMethod);
                throw new UnsupportedOperationException("HTTP method is not supported. Method:" + httpMethod);
            }
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}