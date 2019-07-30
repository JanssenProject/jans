/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.gluu.oxeleven.model.SignRequestParam;

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
