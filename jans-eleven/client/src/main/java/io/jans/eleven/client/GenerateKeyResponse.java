/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import static io.jans.eleven.model.GenerateKeyResponseParam.ALGORITHM;
import static io.jans.eleven.model.GenerateKeyResponseParam.CERTIFICATE_CHAIN;
import static io.jans.eleven.model.GenerateKeyResponseParam.CURVE;
import static io.jans.eleven.model.GenerateKeyResponseParam.EXPIRATION_TIME;
import static io.jans.eleven.model.GenerateKeyResponseParam.EXPONENT;
import static io.jans.eleven.model.GenerateKeyResponseParam.KEY_ID;
import static io.jans.eleven.model.GenerateKeyResponseParam.KEY_TYPE;
import static io.jans.eleven.model.GenerateKeyResponseParam.KEY_USE;
import static io.jans.eleven.model.GenerateKeyResponseParam.MODULUS;
import static io.jans.eleven.model.GenerateKeyResponseParam.X;
import static io.jans.eleven.model.GenerateKeyResponseParam.Y;

import java.util.List;

import io.jans.eleven.util.StringUtils;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

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

    public GenerateKeyResponse(Response clientResponse) {
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
