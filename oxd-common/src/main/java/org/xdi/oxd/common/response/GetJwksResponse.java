/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * JSON Web Key Set response class
 *
 * @author Shoeb
 * @version 11/10/2018
 */
public class GetJwksResponse implements IOpResponse {

    @JsonProperty(value = "jwks")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "jwks")
    private String jwks;

    public GetJwksResponse() {
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }
}