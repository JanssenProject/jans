/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientResponse;
import org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType;
import org.json.JSONException;
import org.json.JSONObject;

import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationResponseParam.*;

/**
 * Represents a CIBA backchannel authorization response.
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class BackchannelAuthenticationResponse extends BaseResponseWithErrors<BackchannelAuthenticationErrorResponseType> {

    private static final Logger LOG = Logger.getLogger(BackchannelAuthenticationResponse.class);

    private String authReqId;
    private Integer expiresIn;
    private Integer interval;

    /**
     * Constructs a backchannel authentication response.
     */
    public BackchannelAuthenticationResponse() {
    }

    /**
     * Constructs a backchannel authentication response.
     */
    public BackchannelAuthenticationResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);
    }

    @Override
    public BackchannelAuthenticationErrorResponseType fromString(String p_str) {
        return BackchannelAuthenticationErrorResponseType.fromString(p_str);
    }

    public void injectDataFromJson() {
        injectDataFromJson(entity);
    }

    @Override
    public void injectDataFromJson(String p_json) {
        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(AUTH_REQ_ID)) {
                    setAuthReqId(jsonObj.getString(AUTH_REQ_ID));
                }
                if (jsonObj.has(EXPIRES_IN)) {
                    setExpiresIn(jsonObj.getInt(EXPIRES_IN));
                }
                if (jsonObj.has(INTERVAL)) {
                    setInterval(jsonObj.getInt(INTERVAL));
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }
}