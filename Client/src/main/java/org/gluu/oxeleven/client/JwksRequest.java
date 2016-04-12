/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.gluu.oxeleven.model.Jwks;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class JwksRequest extends BaseRequest {

    private Jwks jwks;

    public JwksRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setHttpMethod(HttpMethod.POST);
    }

    public Jwks getJwks() {
        return jwks;
    }

    public void setJwks(Jwks jwks) {
        this.jwks = jwks;
    }
}
