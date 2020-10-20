/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.jwk.JSONWebKey;

import java.util.List;

/**
 * JSON Web Key Set response class
 *
 * @author Shoeb
 * @version 12/01/2018
 */


public class GetJwksResponse implements IOpResponse {

    @JsonProperty(value = "keys" )
    private List<JSONWebKey> keys;

    public GetJwksResponse() {
    }

    public List<JSONWebKey> getKeys() {
        return keys;
    }

    public void setKeys(List<JSONWebKey> keys) {
        this.keys = keys;
    }

}


