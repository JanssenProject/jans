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
import javax.faces.view.facelets.ResourceResolver;

import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.xdi.util.StringHelper;

/**
 * External resource handler to customize applicaton 
 *
 * @author Yuriy Movchan Date: 04/05/2016
 */
public class ExternalResourceHandler extends ResourceResolver {

	private static final LogProvider log = Logging.getLogProvider(ExternalResourceHandler.class);

    private ResourceResolver parent;

    private File externalResourceBaseFolder;
	private boolean useExternalResourceBase;

    public ExternalResourceHandler(ResourceResolver parent) {
        this.parent = parent;

		String externalResourceBase = System.getProperty("catalina.base");
		if (StringHelper.isNotEmpty(externalResourceBase)) {
			externalResourceBase += "/pages";
			File folder = new File(externalResourceBase);
			if (folder.exists() && folder.isDirectory()) {
				this.externalResourceBaseFolder = folder;
				this.useExternalResourceBase = true;
			} else {
				log.error("Specified path '" + externalResourceBase + "' in 'catalina.base' not exists or not a folder!");
			}
		}
    }

	@Override
	public URL resolveUrl(String path) {
		if (!useExternalResourceBase) {
			return this.parent.resolveUrl(path);
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
		return this.parent.resolveUrl(path);
	}

	public String toString() {
		return "ExternalResourceHandler";
	}

}
