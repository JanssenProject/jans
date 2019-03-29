package org.xdi.oxauth.audit.debug.wrapper;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Created by eugeniuparvan on 5/10/17.
 */
public class ResponseWrapper extends HttpServletResponseWrapper {

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response
     * @throws IllegalArgumentException if the response is null
     */
    public ResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<String, String>(0);
        for (String headerName : getHeaderNames()) {
            headers.put(headerName, getHeader(headerName));
        }
        return headers;
    }
}
