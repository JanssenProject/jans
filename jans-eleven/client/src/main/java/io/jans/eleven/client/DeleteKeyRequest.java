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

import io.jans.eleven.model.DeleteKeyRequestParam;
import io.jans.eleven.util.StringUtils;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public class DeleteKeyRequest extends BaseRequest {

    private String alias;

    public DeleteKeyRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpMethod(HttpMethod.POST);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            if (!Strings.isNullOrEmpty(alias)) {
                queryStringBuilder.append(DeleteKeyRequestParam.KEY_ID)
                        .append("=").append(URLEncoder.encode(alias, StringUtils.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}
