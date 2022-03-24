/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;

import io.jans.eleven.model.SignRequestParam;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
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

    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        /*try {
            if (signRequestParam != null) {
                queryStringBuilder.append(SignRequestParam.)
                        .append("=").append(URLEncoder.encode(signRequestParam, StringUtils.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/

        return queryStringBuilder.toString();
    }
}
