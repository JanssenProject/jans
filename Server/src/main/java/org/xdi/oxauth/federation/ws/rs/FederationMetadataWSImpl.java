/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.federation.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.signature.RSAKeyFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.federation.FederationErrorResponseType;
import org.xdi.oxauth.model.federation.FederationMetadata;
import org.xdi.oxauth.model.federation.FederationOP;
import org.xdi.oxauth.model.federation.FederationRP;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.service.FederationMetadataService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides implementation of Federation Metadata REST web services interface.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/09/2012
 */
@Name("federationMetadataWS")
public class FederationMetadataWSImpl implements FederationMetadataWS {

    @Logger
    private Log log;

    @In
    private FederationMetadataService federationMetadataService;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @Override
    public Response requestMetadata(String federationId, String signed, HttpServletRequest request, SecurityContext sec) {
        log.debug("Called federation metadata endpoint federation_id: {0}", federationId);

        try {
            if (isRequestValid(request)) {
                if (StringUtils.isBlank(federationId)) {
                    final String entity = asJSON(federationMetadataService.getMetadataList());
                    return Response.status(Response.Status.OK).
                            entity(entity).build();
                } else {
                    final FederationMetadata metadata = federationMetadataService.getMetadata(federationId, true);
                    final String entity = Boolean.FALSE.toString().equalsIgnoreCase(signed) ?
                            asJSON(metadata).toString() : asSignedJSON(metadata);
                    return Response.status(Response.Status.OK).entity(entity).build();
                }
            }
        } catch (FederationMetadataService.InvalidIdException e) {
            return errorResponse(FederationErrorResponseType.INVALID_FEDERATION_ID);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return errorResponse(FederationErrorResponseType.INVALID_REQUEST);
    }

    public Response errorResponse(FederationErrorResponseType p_type) {
        return Response.status(Response.Status.BAD_REQUEST).
                entity(errorResponseFactory.getErrorResponse(p_type).toJSonString()).
                build();
    }

    private String asSignedJSON(FederationMetadata p_metadata) throws JSONException, InvalidJwtException {
        try {
            final String keyId = ConfigurationFactory.instance().getConfiguration().getFederationSigningKid();
            final SignatureAlgorithm algorithm = SignatureAlgorithm.fromName(ConfigurationFactory.instance().getConfiguration().getFederationSigningAlg());

            final JSONWebKey JSONWebKey = ConfigurationFactory.instance().getWebKeys().getKey(keyId);
            final RSAKeyFactory factory = RSAKeyFactory.valueOf(JSONWebKey);

            final JSONObject jsonHeader = JwtHeader.instance().
                    setType(JwtType.JWS).setAlgorithm(algorithm).setKeyId(keyId).
                    toJsonObject();
            final JSONObject jsonPayload = asJSON(p_metadata);
            return JwtUtil.encodeJwt(jsonHeader, jsonPayload, algorithm, factory.getPrivateKey());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return asJSON(p_metadata).toString(); // in case we failed to sign it for some return plain json
        }
    }

    private static JSONObject asJSON(FederationMetadata p_metadata) throws JSONException {
        final JSONArray rps = new JSONArray();
        if (p_metadata.getRpList() != null) {
            for (FederationRP rp : p_metadata.getRpList()) {
                final JSONObject j = new JSONObject();
                j.put("display_name", rp.getDisplayName());

                // redirect_uri
                final List<String> redirectUris = rp.getRedirectUri();
                if (redirectUris != null && !redirectUris.isEmpty()) {
                    final int size = redirectUris.size();
                    if (size == 1) {
                        j.put("redirect_uri", redirectUris.get(0));
                    } else {
                        j.put("redirect_uri", redirectUris);
                    }
                }
                rps.put(j);
            }
        }

        final JSONArray ops = new JSONArray();
        if (p_metadata.getOpList() != null) {
            for (FederationOP op : p_metadata.getOpList()) {
                final JSONObject j = new JSONObject();
                j.put("display_name", op.getDisplayName());
                j.put("op_id", op.getOpId());
                j.put("domain", op.getDomain());

                ops.put(j);
            }
        }

        final JSONObject result = new JSONObject();
        result.put("federation_id", p_metadata.getId());
        result.put("display_name", p_metadata.getDisplayName());
        result.put("interval_check", p_metadata.getIntervalCheck());
        result.put("RPs", rps);
        result.put("OPs", ops);
        return result;
    }

    private static String asJSON(List<FederationMetadata> p_list) {
        final JSONArray array = new JSONArray();
        if (p_list != null && !p_list.isEmpty()) {
            for (FederationMetadata m : p_list) {
                array.put(m.getId());
            }
        }
        return array.toString();
    }


    /**
     * Returns whether request is valid. It's valid ONLY if it has no parameters or have ONLY two "federation_id" and "signed" parameters.
     *
     * @param request request
     * @return whether request is valid
     */
    private static boolean isRequestValid(HttpServletRequest request) {
        if (request != null) {
            final Map<String, String> map = request.getParameterMap();
            if (map != null) {
                final Set<String> keys = map.keySet();
                if (keys != null) {
                    keys.remove("federation_id");
                    keys.remove("signed");

                    // valid if it contains only "federation_id"
                    return keys.isEmpty();
                }
                // it's valid to have no parameters, then return ids of metadata
                return true;
            }
        }
        return false;
    }
}
