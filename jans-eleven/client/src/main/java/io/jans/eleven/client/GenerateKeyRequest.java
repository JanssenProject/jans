/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;

import io.jans.eleven.model.GenerateKeyRequestParam;
import io.jans.eleven.util.StringUtils;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public class GenerateKeyRequest extends BaseRequest {

    private String signatureAlgorithm;
    private Long expirationTime;

    public GenerateKeyRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpMethod(HttpMethod.POST);
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            if (!Strings.isNullOrEmpty(signatureAlgorithm)) {
                queryStringBuilder.append(GenerateKeyRequestParam.SIGNATURE_ALGORITHM)
                        .append("=").append(URLEncoder.encode(signatureAlgorithm, StringUtils.UTF8_STRING_ENCODING));
            }
            if (expirationTime != null) {
                queryStringBuilder.append("&").append(GenerateKeyRequestParam.EXPIRATION_TIME)
                        .append("=").append(expirationTime);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}
