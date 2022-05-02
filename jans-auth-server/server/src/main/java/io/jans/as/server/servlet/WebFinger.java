/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.discovery.OpenIdConnectDiscoveryParamsValidator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static io.jans.as.model.discovery.WebFingerParam.HREF;
import static io.jans.as.model.discovery.WebFingerParam.LINKS;
import static io.jans.as.model.discovery.WebFingerParam.REL;
import static io.jans.as.model.discovery.WebFingerParam.REL_VALUE;
import static io.jans.as.model.discovery.WebFingerParam.RESOURCE;
import static io.jans.as.model.discovery.WebFingerParam.SUBJECT;
import static io.jans.as.model.util.Util.escapeLog;

/**
 * @author Javier Rojas Blum Date: 01.28.2013
 */
@WebServlet(urlPatterns = "/.well-known/webfinger")
public class WebFinger extends HttpServlet {

    private static final long serialVersionUID = -4708834950205359151L;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws java.io.IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/jrd+json");

        String resource = request.getParameter(RESOURCE);
        String rel = request.getParameter(REL);

        log.debug("Attempting to request OpenID Connect Discovery: {}, {}", escapeLog(resource), escapeLog(rel));

        try (PrintWriter out = response.getWriter()) {
            if (!OpenIdConnectDiscoveryParamsValidator.validateParams(resource, rel)) {
                return;
            }

            if (rel == null || rel.equals(REL_VALUE)) {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(SUBJECT, resource);

                JSONArray linksJsonArray = new JSONArray();
                JSONObject linkJsonObject = new JSONObject();
                linkJsonObject.put(REL, REL_VALUE);
                linkJsonObject.put(HREF, appConfiguration.getIssuer());

                linksJsonArray.put(linkJsonObject);
                jsonObj.put(LINKS, linksJsonArray);

                out.println(jsonObj.toString(4).replace("\\/", "/"));
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws jakarta.servlet.ServletException if a servlet-specific error occurs
     * @throws java.io.IOException            if an I/O error occurs
     */
    @Override
    protected void doGet
    (HttpServletRequest
             request, HttpServletResponse
             response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost
    (HttpServletRequest
             request, HttpServletResponse
             response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "OpenID Connect Discovery";
    }
}