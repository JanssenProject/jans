/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.util.process;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import com.sun.facelets.FaceletViewHandler;

public class CustomViewHandler extends FaceletViewHandler {

	public CustomViewHandler(ViewHandler parent) {
		super(parent);
	}

	protected void handleRenderException(FacesContext context, Exception ex) throws IOException, ELException, FacesException {
		try {
			String errorPage = context.getExternalContext().getRequestContextPath() + "/error.htm";
			if (context.getViewRoot().getViewId().equals(errorPage)) {
				return;
			}

			context.getExternalContext().getSessionMap().put("javax.servlet.error.exception", ex);
			System.out.println("Caught error: " + ex);
			ex.printStackTrace();
			((HttpServletResponse) context.getExternalContext().getResponse()).sendRedirect(errorPage);

		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

}
