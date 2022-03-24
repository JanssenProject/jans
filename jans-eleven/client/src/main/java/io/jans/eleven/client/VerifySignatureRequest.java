/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;

import io.jans.eleven.model.VerifySignatureRequestParam;

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
