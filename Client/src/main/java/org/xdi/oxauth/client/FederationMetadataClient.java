/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientRequest;
import org.xdi.oxauth.model.federation.FederationMetadata;
import org.xdi.oxauth.model.federation.FederationOP;
import org.xdi.oxauth.model.federation.FederationRP;
import org.xdi.oxauth.model.jwt.PureJwt;
import org.xdi.oxauth.model.util.Util;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/09/2012
 */

public class FederationMetadataClient extends BaseClient<FederationMetadataRequest, FederationMetadataResponse> {

    private static final Logger LOG = Logger.getLogger(FederationMetadataClient.class);

    public FederationMetadataClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    public FederationMetadataResponse execGetMetadataIds() {
        return exec(new FederationMetadataRequest());
    }

    public FederationMetadataResponse execGetMetadataById(String p_federationMetadataId) {
        return exec(new FederationMetadataRequest(p_federationMetadataId));
    }

    public FederationMetadataResponse exec(FederationMetadataRequest p_request) {
        setResponse(new FederationMetadataResponse());
        final String httpMethod = getHttpMethod();

        initClientRequest();
        clientRequest.header("Content-Type", MediaType.APPLICATION_JSON);
        clientRequest.setHttpMethod(httpMethod);

        try {
            if (HttpMethod.GET.equals(httpMethod)) { // TODO: Ask Yuriy Z. about GET == GET
                prepareRequest(p_request, clientRequest);

                clientResponse = clientRequest.get(String.class);

                setRequest(p_request);

                final String entity = clientResponse.getEntity(String.class);

                getResponse().setStatus(clientResponse.getStatus());
                getResponse().setHeaders(clientResponse.getHeaders());
                getResponse().setLocation(clientResponse.getLocation() != null ? clientResponse.getLocation().getHref() : "");

                fillResponse(getResponse(), entity, p_request.isSigned());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    private static void prepareRequest(FederationMetadataRequest p_request, ClientRequest p_clientRequest) {
        if (StringUtils.isNotBlank(p_request.getFederationId())) {
            p_clientRequest.queryParameter("federation_id", p_request.getFederationId());
        }
        if (!p_request.isSigned()) {
            p_clientRequest.queryParameter("signed", p_request.isSigned());
        }
    }

    public static void fillResponse(FederationMetadataResponse p_response, String entity, boolean p_signed) {
        p_response.setEntity(entity);

        if (StringUtils.isNotBlank(entity)) {
            p_response.injectErrorIfExistSilently(entity);

            if (p_signed) {
                final PureJwt jwt = PureJwt.parse(entity);
                if (jwt != null) {
                    final String decodedPayload = jwt.getDecodedPayload();

                    // try whether its json object, details of metadata or error response
                    try {
                        p_response.setMetadata(convertMetadata(new JSONObject(decodedPayload)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // try whether its json array (it can be also object) -> if yes then it's metadata ids
                    try {
                        p_response.setExistingMetadataIdList(convertMetadataIdList(entity));
                    } catch (JSONException e) {
                        // skip, it's ok here because it can be array or object
                    }
                }
            } else {
                // try whether its json array (it can be also object) -> if yes then it's metadata ids
                try {
                    p_response.setExistingMetadataIdList(convertMetadataIdList(entity));
                } catch (JSONException e) {
                    // skip, it's ok here because it can be array or object
                }

                // try whether its json object, details of metadata or error response
                try {
                    p_response.setMetadata(convertMetadata(new JSONObject(entity)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static FederationMetadata convertMetadata(JSONObject p_jsonObj) throws JSONException {
        final FederationMetadata metadata = new FederationMetadata();
        if (p_jsonObj.has("federation_id")) {
            metadata.setId(p_jsonObj.getString("federation_id"));
        }
        if (p_jsonObj.has("display_name")) {
            metadata.setDisplayName(p_jsonObj.getString("display_name"));
        }
        if (p_jsonObj.has("interval_check")) {
            metadata.setIntervalCheck(p_jsonObj.getString("interval_check"));
        }
        if (p_jsonObj.get("OPs") instanceof JSONArray) {
            final List<FederationOP> opList = new ArrayList<FederationOP>();
            metadata.setOpList(opList);

            final JSONArray oPs = (JSONArray) p_jsonObj.get("OPs");
            for (int i = 0; i < oPs.length(); i++) {
                opList.add(convertOP(oPs.getJSONObject(i)));
            }
        }
        if (p_jsonObj.get("RPs") instanceof JSONArray) {
            final List<FederationRP> rpList = new ArrayList<FederationRP>();
            metadata.setRpList(rpList);

            final JSONArray rPs = (JSONArray) p_jsonObj.get("RPs");
            for (int i = 0; i < rPs.length(); i++) {
                rpList.add(convertRP(rPs.getJSONObject(i)));
            }
        }

        return metadata;
    }

    public static List<String> convertMetadataIdList(String p_jsonAsString) throws JSONException {
        final JSONArray jsonArray = new JSONArray(p_jsonAsString);
        return Util.asList(jsonArray);
    }

    public static FederationOP convertOP(JSONObject p_json) throws JSONException {
        final FederationOP op = new FederationOP();
        if (p_json != null) {
            if (p_json.has("display_name")) {
                op.setDisplayName(p_json.getString("display_name"));
            }
            if (p_json.has("op_id")) {
                op.setOpId(p_json.getString("op_id"));
            }
            if (p_json.has("domain")) {
                op.setDomain(p_json.getString("domain"));
            }
        }
        return op;
    }

    public static FederationRP convertRP(JSONObject p_json) throws JSONException {
        final FederationRP rp = new FederationRP();
        if (p_json != null) {
            if (p_json.has("display_name")) {
                rp.setDisplayName(p_json.getString("display_name"));
            }
            if (p_json.has("redirect_uri")) {
                final List<String> redirectUriList = new ArrayList<String>();
                rp.setRedirectUri(redirectUriList);

                final Object redirectUriObject = p_json.get("redirect_uri");
                if (redirectUriObject instanceof String) {
                    redirectUriList.add((String) redirectUriObject);
                } else if (redirectUriObject instanceof JSONArray) {
                    final JSONArray array = (JSONArray) redirectUriObject;
                    for (int i = 0; i < array.length(); i++) {
                        redirectUriList.add(array.getString(i));
                    }
                } else if (redirectUriObject != null) {
                    redirectUriList.add(redirectUriObject.toString());
                }
            }
        }
        return rp;
    }
}