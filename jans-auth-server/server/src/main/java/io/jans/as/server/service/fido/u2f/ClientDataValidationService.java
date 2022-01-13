/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * Client data validation service
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
@Stateless
@Named
public class ClientDataValidationService {

    @Inject
    private Logger log;

    public void checkContent(ClientData clientData, String[] types, String challenge, Set<String> facets) throws BadInputException {
        if (!ArrayUtils.contains(types, clientData.getTyp())) {
            throw new BadInputException("Bad clientData: wrong typ " + clientData.getTyp());
        }

        if (!challenge.equals(clientData.getChallenge())) {
            throw new BadInputException("Bad clientData: wrong challenge");
        }

        if (facets != null && !facets.isEmpty()) {
            Set<String> allowedFacets = canonicalizeOrigins(facets);
            String canonicalOrigin;
            try {
                canonicalOrigin = canonicalizeOrigin(clientData.getOrigin());
            } catch (RuntimeException e) {
                throw new BadInputException("Bad clientData: Malformed origin", e);
            }
            verifyOrigin(canonicalOrigin, allowedFacets);
        }
    }

    private static void verifyOrigin(String origin, Set<String> allowedOrigins) throws BadInputException {
        if (!allowedOrigins.contains(origin)) {
            throw new BadInputException(origin + " is not a recognized facet for this application");
        }
    }

    public static Set<String> canonicalizeOrigins(Set<String> origins) {
        Set<String> result = new HashSet<String>();
        for (String origin : origins) {
            result.add(canonicalizeOrigin(origin));
        }
        return result;
    }

    public static String canonicalizeOrigin(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getAuthority() == null) {
                return url;
            }
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Specified bad origin", e);
        }
    }

}
