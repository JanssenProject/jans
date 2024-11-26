/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalDiscoveryService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.util.ServerUtil;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2021
 */
@WebServlet(urlPatterns = "/.well-known/openid-configuration", loadOnStartup = 10)
public class OpenIdConfiguration extends HttpServlet {

    private static final long serialVersionUID = -8224898157373678904L;

    @Inject
    private transient Logger log;

    @Inject
    private transient DiscoveryService discoveryService;

    @Inject
    private transient ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private transient ExternalDynamicScopeService externalDynamicScopeService;

    @Inject
    private transient ExternalDiscoveryService externalDiscoveryService;

    @Inject
    private transient LocalResponseCache localResponseCache;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     * @throws IOException
     */
    @SuppressWarnings({"deprecation", "java:S3776"})
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) throws IOException {
        if (!(externalAuthenticationService.isLoaded() && externalDynamicScopeService.isLoaded())) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            log.error("Jans Auth still starting up!");
            return;
        }

        httpResponse.setContentType("application/json");
        try (PrintWriter out = httpResponse.getWriter()) {
            final JSONObject cachedResponse = localResponseCache.getDiscoveryResponse();
            if (cachedResponse != null) {
                log.trace("Cached discovery response returned.");
                out.println(ServerUtil.toPrettyJson(cachedResponse).replace("\\/", "/"));
                return;
            }

            JSONObject jsonObj = discoveryService.process();
            JSONObject clone = new JSONObject(jsonObj.toString());

            ExecutionContext context = new ExecutionContext(servletRequest, httpResponse);
            if (!externalDiscoveryService.modifyDiscovery(jsonObj, context)) {
                jsonObj = clone; // revert to original state if object was modified in script
            }

            out.println(ServerUtil.toPrettyJson(jsonObj).replace("\\/", "/"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "OpenID Provider Configuration Information";
    }

    public static void filterOutKeys(JSONObject jsonObj, AppConfiguration appConfiguration) {

        // filter out keys with blank values
        if (BooleanUtils.isFalse(appConfiguration.getAllowBlankValuesInDiscoveryResponse())) {
            for (String key : new HashSet<>(jsonObj.keySet())) {
                if (jsonObj.get(key) == null || StringUtils.isBlank(jsonObj.optString(key))) {
                    jsonObj.remove(key);
                }
            }
        }

        final List<String> denyKeys = appConfiguration.getDiscoveryDenyKeys();
        if (!denyKeys.isEmpty()) {
            for (String key : new HashSet<>(jsonObj.keySet())) {
                if (denyKeys.contains(key)) {
                    jsonObj.remove(key);
                }
            }
        }

        final List<String> allowedKeys = appConfiguration.getDiscoveryAllowedKeys();
        if (!allowedKeys.isEmpty()) {
            for (String key : new HashSet<>(jsonObj.keySet())) {
                if (!allowedKeys.contains(key)) {
                    jsonObj.remove(key);
                }
            }
        }
    }

}