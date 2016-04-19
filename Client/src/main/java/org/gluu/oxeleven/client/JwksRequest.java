/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.gluu.oxeleven.model.JwksRequestParam;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
 */
public class JwksRequest extends BaseRequest {

    private JwksRequestParam jwksRequestParam;

    public JwksRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setHttpMethod(HttpMethod.POST);
    }

    public JwksRequestParam getJwksRequestParam() {
        return jwksRequestParam;
    }

    public void setJwksRequestParam(JwksRequestParam jwksRequestParam) {
        this.jwksRequestParam = jwksRequestParam;
    }
}
