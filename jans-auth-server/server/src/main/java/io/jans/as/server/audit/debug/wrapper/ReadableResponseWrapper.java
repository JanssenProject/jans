/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.audit.debug.wrapper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ReadableResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter writer;
    private HttpServletResponse response;
    private static final Logger LOG = Logger.getLogger(ReadableResponseWrapper.class);

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response
     * @throws IllegalArgumentException if the response is null
     */
    public ReadableResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
        writer = new CharArrayWriter();
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<String, String>(0);
        for (String headerName : getHeaderNames()) {
            headers.put(headerName, getHeader(headerName));
        }
        return headers;
    }

    public PrintWriter getWriter() {
        return new PrintWriter(writer);
    }

    public String readBodyValue() {
        String value = writer.toString();
        try {
            PrintWriter outWriter = response.getWriter();
            writer.writeTo(outWriter);
            outWriter.close();
        } catch (Exception e) {
            LOG.error("Error WRITING body again to response servlet ; " + e.getMessage());
        }
        if (value == null || value.isEmpty()) {
            return "empty";
        } else if (getHeaders().containsKey("Content-Type") && getHeaders().get("Content-Type").contains("application/json")) {
            return readJson(value);
        } else {
            return value;
        }
    }

    private String readJson(String value) {
        try {
            if (value.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(value);
                return jsonArray.toString();
            } else {
                JSONObject jsonObject = new JSONObject(value);
                return jsonObject.toString();
            }
        } catch (Exception e) {
            LOG.error("Error reading body value from response ; " + e.getMessage());
        }
        return value;
    }

}
