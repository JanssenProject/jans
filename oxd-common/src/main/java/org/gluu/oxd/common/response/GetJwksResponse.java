/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;
import org.gluu.oxauth.model.jwk.JSONWebKey;

import java.util.List;

/**
 * JSON Web Key Set response class
 *
 * @author Shoeb
 * @version 12/01/2018
 */


public class GetJwksResponse implements IOpResponse {

    @JsonProperty(value = "keys" )
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keys")
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


