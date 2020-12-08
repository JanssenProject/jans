/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package com.toopher;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import java.io.IOException;

/**
 * Request errors from API calls
 * 
 */
public class RequestError extends Exception {

    public RequestError(ClientProtocolException e) {
        super("Http protocol error", e);
    }

    public RequestError(IOException e) {
        super("Connection error", e);
    }

    public RequestError(JSONException e) {
        super("Unexpected response format", e);
    }

    public RequestError(Exception e) {
        super("Request error", e);
    }

    private static final long serialVersionUID = -1479647692976296897L;
}
