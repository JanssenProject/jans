/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.servlet;

import static org.xdi.oxauth.model.discovery.WebFingerParam.HREF;
import static org.xdi.oxauth.model.discovery.WebFingerParam.LINKS;
import static org.xdi.oxauth.model.discovery.WebFingerParam.REL;
import static org.xdi.oxauth.model.discovery.WebFingerParam.REL_VALUE;
import static org.xdi.oxauth.model.discovery.WebFingerParam.RESOURCE;
import static org.xdi.oxauth.model.discovery.WebFingerParam.SUBJECT;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.discovery.OpenIdConnectDiscoveryParamsValidator;

/**
 * @author Javier Rojas Blum Date: 01.28.2013
 */
public class WebFinger extends HttpServlet {

    private static final Logger logger = Logger.getLogger(WebFinger.class);

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException if a servlet-specific error occurs
     * @throws java.io.IOException            if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/jrd+json");
        PrintWriter out = response.getWriter();

        String resource = request.getParameter(RESOURCE);
        String rel = request.getParameter(REL);

        logger.debug("Attempting to request OpenID Connect Discovery: " + resource + ", " + rel + ", Is Secure = " + request.isSecure());

        try {
            if (OpenIdConnectDiscoveryParamsValidator.validateParams(resource, rel)) {
                if (rel == null || rel.equals(REL_VALUE)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put(SUBJECT, resource);

                    JSONArray linksJsonArray = new JSONArray();
                    JSONObject linkJsonObject = new JSONObject();
                    linkJsonObject.put(REL, REL_VALUE);
                    linkJsonObject.put(HREF, ConfigurationFactory.instance().getConfiguration().getIssuer());

                    linksJsonArray.put(linkJsonObject);
                    jsonObj.put(LINKS, linksJsonArray);

                    out.println(jsonObj.toString(4).replace("\\/", "/"));
                }
            }
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }

        out.close();
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException if a servlet-specific error occurs
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