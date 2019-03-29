package org.xdi.oxauth.audit.debug.entity;

import java.util.Map;

/**
 * Created by eugeniuparvan on 5/15/17.
 */
public class HttpResponse {
    private int status;
    private Map<String, String> headers;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
