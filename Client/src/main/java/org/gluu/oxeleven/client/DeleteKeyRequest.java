/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.gluu.oxeleven.model.DeleteKeyRequestParam;
import org.gluu.oxeleven.util.StringUtils;

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
