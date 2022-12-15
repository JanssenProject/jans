/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import io.jans.as.server.exception.fido.u2f.BadConfigurationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides operations with U2F applications
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@ApplicationScoped
public class ApplicationService {

    private static final boolean VALIDATE_APPLICATION = true;

    public boolean isValidateApplication() {
        return VALIDATE_APPLICATION;
    }

    /**
     * Throws {@link BadConfigurationException} if the given App ID is found to
     * be incompatible with the U2F specification or any major U2F Client
     * implementation.
     *
     * @param appId the App ID to be validated
     */
    public void checkIsValid(String appId) {
        if (!appId.contains(":")) {
            throw new BadConfigurationException("App ID does not look like a valid facet or URL. Web facets must start with 'https://'.");
        }

        if (appId.startsWith("http:")) {
            throw new BadConfigurationException("HTTP is not supported for App IDs. Use HTTPS instead.");
        }

        if (appId.startsWith("https://")) {
            URI url = checkValidUrl(appId);
            checkPathIsNotSlash(url);
        }
    }

    private void checkPathIsNotSlash(URI url) {
        if ("/".equals(url.getPath())) {
            throw new BadConfigurationException(
                    "The path of the URL set as App ID is '/'. This is probably not what you want -- remove the trailing slash of the App ID URL.");
        }
    }

    private URI checkValidUrl(String appId) {
        URI url = null;
        try {
            url = new URI(appId);
        } catch (URISyntaxException e) {
            throw new BadConfigurationException("App ID looks like a HTTPS URL, but has syntax errors.", e);
        }
        return url;
    }
}
