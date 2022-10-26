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

@WebServlet(urlPatterns = "/servlet/logo")
public class LogoServlet extends HttpServlet {

    private static final long serialVersionUID = 5445488800130871634L;

    private static final Logger log = LoggerFactory.getLogger(LogoServlet.class);

    public static final String BASE_OXAUTH_LOGO_PATH = "/opt/gluu/jetty/jans-auth/custom/static/logo/";

    @Inject
    private OrganizationService organizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("/image/jpg");
        response.setDateHeader("Expires", new Date().getTime() + 1000L * 1800);
        GluuOrganization organization = organizationService.getOrganization();
        boolean hasSucceed = readCustomLogo(response, organization);
        if (!hasSucceed) {
            readDefaultLogo(response);
        }
    }

    private boolean readDefaultLogo(HttpServletResponse response) {
        String defaultLogoFileName = "/WEB-INF/static/logo.png";
        try (InputStream in = getServletContext().getResourceAsStream(defaultLogoFileName);
             OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("---------------Error loading default logo: " + e.getMessage());
            return false;
        }
    }

    private boolean readCustomLogo(HttpServletResponse response, GluuOrganization organization) {
        if (organization.getJsLogoPath() == null || StringUtils.isEmpty(organization.getJsLogoPath())) {
            return false;
        }
        File directory = new File(BASE_OXAUTH_LOGO_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File logoPath = new File(organization.getJsLogoPath());
        if (!logoPath.exists()) {
            return false;
        }
        try (InputStream in = new FileInputStream(logoPath); OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading custom logo: " + e.getMessage());
            return false;
        }
    }
}
