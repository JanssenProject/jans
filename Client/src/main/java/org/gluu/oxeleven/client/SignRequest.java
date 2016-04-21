/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.gluu.oxeleven.model.SignRequestParam;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version April 19, 2016
 */
public class SignRequest extends BaseRequest {

    private SignRequestParam signRequestParam;

    public SignRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setHttpMethod(HttpMethod.POST);

        signRequestParam = new SignRequestParam();
    }

    public SignRequestParam getSignRequestParam() {
        return signRequestParam;
    }

    public void setSignRequestParam(SignRequestParam signRequestParam) {
        this.signRequestParam = signRequestParam;
    }
}
