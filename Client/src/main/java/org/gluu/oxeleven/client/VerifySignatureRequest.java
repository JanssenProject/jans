/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.gluu.oxeleven.model.VerifySignatureRequestParam;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
 */
public class VerifySignatureRequest extends BaseRequest {

    private VerifySignatureRequestParam verifySignatureRequestParam;

    public VerifySignatureRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setHttpMethod(HttpMethod.POST);

        verifySignatureRequestParam = new VerifySignatureRequestParam();
    }

    public VerifySignatureRequestParam getVerifySignatureRequestParam() {
        return verifySignatureRequestParam;
    }

    public void setVerifySignatureRequestParam(VerifySignatureRequestParam verifySignatureRequestParam) {
        this.verifySignatureRequestParam = verifySignatureRequestParam;
    }
}
