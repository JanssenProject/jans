package com.toopher;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

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
