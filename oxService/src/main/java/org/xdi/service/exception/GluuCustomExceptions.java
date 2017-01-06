package org.xdi.service.exception;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.Interpolator;
import org.jboss.seam.exception.Exceptions;
import org.jboss.seam.util.Strings;
import org.xdi.facelet.CustomRendererRequest;

/**
 * Allows to extends the exception handler chain
 * 
 * @author Yuriy Movchan
 * @version January 05, 2017
 */
@Scope(ScopeType.APPLICATION)
@BypassInterceptors
@Install(precedence = Install.APPLICATION, classDependencies = "javax.faces.context.FacesContext")
@Name("org.jboss.seam.exception.exceptions")
public class GluuCustomExceptions extends Exceptions {

	public void handle(Exception e) throws Exception {
		if (handleServiceException(e)) {
			Events.instance().raiseEvent("org.jboss.seam.exceptionHandled." + e.getClass().getName(), e);
			Events.instance().raiseEvent("org.jboss.seam.exceptionHandled", e);
			return;
		}

		super.handle(e);
	}

	public static Exceptions instance() {
		if (!Contexts.isApplicationContextActive()) {
			throw new IllegalStateException("No active application context");
		}

		return (Exceptions) Component.getInstance(GluuCustomExceptions.class, ScopeType.APPLICATION);
	}

	public boolean handleServiceException(Exception ex) throws IOException {
		FacesContext ctx = FacesContext.getCurrentInstance();
		if (ctx == null) {
			return false;
		}

		String servletPath = ctx.getExternalContext().getRequestServletPath();
		if (!servletPath.startsWith("/seam/resource")) {
			return false;
		}

		addErrorMessage(null, ex);

		final HttpServletResponse httpResponse = (HttpServletResponse) ctx.getExternalContext().getResponse();

		CustomRendererRequest customRendererRequest = new CustomRendererRequest("/error_service.xhtml");
		customRendererRequest.run();
		String errorPage = customRendererRequest.getOutput();
		
		httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		httpResponse.setHeader("Expires", "0"); // Proxies.

		httpResponse.setContentType("text/html;charset=UTF-8");
		httpResponse.setStatus(HttpStatus.SC_BAD_REQUEST);

		IOUtils.write(errorPage, httpResponse.getOutputStream());

		return true;
	}

	protected void addErrorMessage(String message, Exception ex) {
		if (Contexts.isConversationContextActive()) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					getDisplayMessage(ex, null), "An unexpected error has occurred."));
		}
	}

	protected String getDisplayMessage(Exception ex, String message) {
		if (Strings.isEmpty(message) && (ex.getMessage() != null)) {
			return ex.getMessage();
		} else {
			return Interpolator.instance().interpolate(message, ex);
		}
	}
}
