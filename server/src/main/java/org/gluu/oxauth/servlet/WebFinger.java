/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.servlet;

import static org.gluu.oxauth.model.discovery.WebFingerParam.HREF;
import static org.gluu.oxauth.model.discovery.WebFingerParam.LINKS;
import static org.gluu.oxauth.model.discovery.WebFingerParam.REL;
import static org.gluu.oxauth.model.discovery.WebFingerParam.REL_VALUE;
import static org.gluu.oxauth.model.discovery.WebFingerParam.RESOURCE;
import static org.gluu.oxauth.model.discovery.WebFingerParam.SUBJECT;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.discovery.OpenIdConnectDiscoveryParamsValidator;
import org.slf4j.Logger;

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
     * @throws javax.servlet.ServletException if a servlet-specific error occurs
     * @throws java.io.IOException            if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final HttpServletRequest httpRequest = request;
        final HttpServletResponse httpResponse = response;

        httpResponse.setContentType("application/jrd+json");
        PrintWriter out = httpResponse.getWriter();

        String resource = httpRequest.getParameter(RESOURCE);
        String rel = httpRequest.getParameter(REL);

        log.debug("Attempting to request OpenID Connect Discovery: " + resource + ", " + rel + ", Is Secure = " + httpRequest.isSecure());

        try {
            if (OpenIdConnectDiscoveryParamsValidator.validateParams(resource, rel)) {
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
            }
        } catch (JSONException e) {
        	log.error(e.getMessage(), e);
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