/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.service.ws.rs;

import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.core.LockApiError;
import io.jans.lock.service.config.ConfigurationService;
import io.jans.lock.util.ServerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Yuriy Movchan Date: 07/10/2024
 */
@WebServlet(urlPatterns = "/.well-known/lock-server-configuration", loadOnStartup = 10)
@Path("/.well-known/lock-server-configuration")
public class WellKnownConfiguration extends HttpServlet {

    private static final long serialVersionUID = -8224898157373678904L;

    @Inject
    private transient Logger log;

    @Inject
    private transient ConfigurationService configurationService;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     * @throws IOException
     */
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) throws IOException {
    	httpResponse.setContentType("application/json");
        try (PrintWriter out = httpResponse.getWriter()) {
            ObjectNode responde = configurationService.getLockConfiguration();
            out.println(ServerUtil.toPrettyJson(responde).replace("\\/", "/"));
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
	@Operation(summary = "Request .well-known data", description = "Request .well-know Lock server configuration", tags = {
			"Lock - Server Configuration" })	
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class, description = "ConfigurationFound"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Override
	public void doGet(@Parameter(hidden = true) HttpServletRequest request, @Parameter(hidden = true) HttpServletResponse response) throws IOException {
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
        return "Lock Well Known Configuration Information";
    }

}