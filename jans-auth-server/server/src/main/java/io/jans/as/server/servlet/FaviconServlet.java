/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.common.service.OrganizationService;
import io.jans.as.persistence.model.GluuOrganization;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

@WebServlet(urlPatterns = "/servlet/favicon")
public class FaviconServlet extends HttpServlet {

    @Inject
    private OrganizationService organizationService;

    private static final long serialVersionUID = 5445488800130871634L;

    private static final Logger log = LoggerFactory.getLogger(FaviconServlet.class);
    public static final String BASE_OXAUTH_FAVICON_PATH = "/opt/gluu/jetty/jans-auth/custom/static/favicon/";

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("image/x-icon");
        response.setDateHeader("Expires", new Date().getTime() + 1000L * 1800);
        GluuOrganization organization = organizationService.getOrganization();
        boolean hasSucceed = readCustomFavicon(response, organization);
        if (!hasSucceed) {
            readDefaultFavicon(response);
        }
    }

    private boolean readDefaultFavicon(HttpServletResponse response) {
        String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";
        try (InputStream in = getServletContext().getResourceAsStream(defaultFaviconFileName);
             OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading default favicon: " + e.getMessage());
            return false;
        }
    }

    private boolean readCustomFavicon(HttpServletResponse response, GluuOrganization organization) {
        if (organization.getJsFaviconPath() == null || StringUtils.isEmpty(organization.getJsFaviconPath())) {
            return false;
        }

        File directory = new File(BASE_OXAUTH_FAVICON_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File faviconPath = new File(organization.getJsFaviconPath());
        if (!faviconPath.exists()) {
            return false;
        }
        try (InputStream in = new FileInputStream(faviconPath); OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading custom favicon: " + e.getMessage());
            return false;
        }
    }
}
