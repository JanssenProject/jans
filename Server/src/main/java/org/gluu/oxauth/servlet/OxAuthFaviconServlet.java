package org.gluu.oxauth.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.GluuOrganization;
import org.gluu.oxauth.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@WebServlet(urlPatterns = "/servlet/favicon")
public class OxAuthFaviconServlet extends HttpServlet {

	@Inject
	private OrganizationService organizationService;

	private static final long serialVersionUID = 5445488800130871634L;

	private static final Logger log = LoggerFactory.getLogger(OxAuthFaviconServlet.class);
	public static final String BASE_OXAUTH_FAVICON_PATH = "/opt/gluu/jetty/oxauth/custom/static/favicon/";

	@Override
	protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("image/x-icon");
		GluuOrganization organization = organizationService.getOrganization();
		boolean hasSucceed = readCustomFavicon(response, organization);
		if (!hasSucceed) {
			readDefaultFavicon(response);
		}
	}

	private boolean readDefaultFavicon(HttpServletResponse response) {
		String defaultFaviconFileName = "/WEB-INF/static/images/favicon_icosahedron.ico";
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
		if (organization.getOxTrustFaviconPath() == null || StringUtils.isEmpty(organization.getOxTrustFaviconPath())) {
			return false;
		}

		File directory = new File(BASE_OXAUTH_FAVICON_PATH);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File faviconPath = new File(BASE_OXAUTH_FAVICON_PATH + organization.getOxTrustFaviconPath());
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
