/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.gluu.oxeleven.model.VerifySignatureRequestParam;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
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

    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        /*try {
            if (verifySignatureRequestParam != null) {
                queryStringBuilder.append(VerifySignatureRequestParam.)
                        .append("=").append(URLEncoder.encode(verifySignatureRequestParam, StringUtils.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/

        return queryStringBuilder.toString();
    }
}
