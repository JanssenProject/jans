/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.server.service.SectorIdentifierService;
import org.json.JSONArray;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@WebServlet(urlPatterns = "/sectoridentifier/*")
public class SectorIdentifier extends HttpServlet {

	private static final long serialVersionUID = -1222077047492070618L;

	@Inject
    private Logger log;

    @Inject
    private SectorIdentifierService sectorIdentifierService;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final HttpServletRequest httpRequest = request;
        final HttpServletResponse httpResponse = response;

        httpResponse.setContentType("application/json");
        PrintWriter out = httpResponse.getWriter();
        try {
            String urlPath = httpRequest.getPathInfo();
            String oxId = urlPath.substring(urlPath.lastIndexOf("/") + 1, urlPath.length());

            io.jans.as.persistence.model.SectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(oxId);

            JSONArray jsonArray = new JSONArray();

            for (String redirectUri : sectorIdentifier.getRedirectUris()) {
                jsonArray.put(redirectUri);
            }

            out.println(jsonArray.toString(4).replace("\\/", "/"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            out.close();
        }
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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
        return "Sector Identifier";
    }
}
