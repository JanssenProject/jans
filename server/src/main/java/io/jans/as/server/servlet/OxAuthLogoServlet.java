/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;

@WebServlet(urlPatterns = "/servlet/logo")
public class OxAuthLogoServlet extends HttpServlet {

	private static final long serialVersionUID = 5445488800130871634L;

	private static final Logger log = LoggerFactory.getLogger(OxAuthLogoServlet.class);

	public static final String BASE_OXAUTH_LOGO_PATH = "/opt/gluu/jetty/oxauth/custom/static/logo/";

	@Inject
	private OrganizationService organizationService;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("/image/jpg");
		response.setDateHeader("Expires", new Date().getTime()+1000L*1800);
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
		if (organization.getOxAuthLogoPath() == null || StringUtils.isEmpty(organization.getOxAuthLogoPath())) {
			return false;
		}
		File directory = new File(BASE_OXAUTH_LOGO_PATH);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File logoPath = new File(organization.getOxAuthLogoPath());
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
