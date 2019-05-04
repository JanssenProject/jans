/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.gluu.oxeleven.util.StringUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.json.JSONObject;

import java.util.List;

import static org.gluu.oxeleven.model.GenerateKeyResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version October 5, 2016
 */
public class GenerateKeyResponse extends BaseResponse {

    private String keyId;
    private String keyType;
    private String keyUse;
    private String algorithm;
    private Long expirationTime;
    private String modulus;
    private String exponent;
    private String curve;
    private String x;
    private String y;
    private List<String> x5c;

    public GenerateKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            keyType = jsonObject.optString(KEY_TYPE);
            keyId = jsonObject.optString(KEY_ID);
            keyUse = jsonObject.optString(KEY_USE);
            algorithm = jsonObject.optString(ALGORITHM);
            modulus = jsonObject.optString(MODULUS);
            exponent = jsonObject.optString(EXPONENT);
            curve = jsonObject.optString(CURVE);
            x = jsonObject.optString(X);
            y = jsonObject.optString(Y);
            expirationTime = jsonObject.optLong(EXPIRATION_TIME);
            x5c = StringUtils.toList(jsonObject.optJSONArray(CERTIFICATE_CHAIN));
        }
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getKeyUse() {
        return keyUse;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public String getModulus() {
        return modulus;
    }

    public String getExponent() {
        return exponent;
    }

    public String getCurve() {
        return curve;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public List<String> getX5c() {
        return x5c;
    }
}
