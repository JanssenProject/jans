/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxauth.resource.custom;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.faces.FacesException;

import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.xdi.util.StringHelper;

import com.sun.facelets.impl.DefaultResourceResolver;

/**
 * External resource handler to customize applicaton 
 *
 * @author Yuriy Movchan Date: 04/05/2016
 */
public class ExternalResourceHandler extends DefaultResourceResolver {

	private static final LogProvider log = Logging.getLogProvider(ExternalResourceHandler.class);

	private File externalResourceBaseFolder;
	private boolean useExternalResourceBase;

	public ExternalResourceHandler() {
		String externalResourceBase = System.getProperty("gluu.external.resource.base");
		if (StringHelper.isNotEmpty(externalResourceBase)) {
			externalResourceBase += "/oxauth/pages";
			File folder = new File(externalResourceBase);
			if (folder.exists() && folder.isDirectory()) {
				this.externalResourceBaseFolder = folder;
				this.useExternalResourceBase = true;
			} else {
				log.error("Specified path '" + externalResourceBase + "' in 'gluu.external.resource.base' not exists or not folder!");
			}
		}
	}

	@Override
	public URL resolveUrl(String path) {
		if (!useExternalResourceBase) {
			return super.resolveUrl(path);
		}

		// First try external resource folder
		final File externalResource = new File(this.externalResourceBaseFolder, path);
		if (externalResource.exists()) {
			try {
				log.debug("Found overriden resource: " + path);
				URL resource = externalResource.toURI().toURL();

				return resource;
			} catch (MalformedURLException ex) {
				throw new FacesException(ex);
			}
		}

		// Return default resource
		return super.resolveUrl(path);
	}

	public String toString() {
		return "ExternalResourceHandler";
	}

}