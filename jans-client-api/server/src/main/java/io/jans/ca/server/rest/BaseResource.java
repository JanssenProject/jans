package io.jans.ca.server.rest;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import org.slf4j.Logger;

public class BaseResource {

    @Inject
    Logger logger;

    @Context
    private HttpServletRequest httpRequest;

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }
}
