/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.net;

import io.jans.net.InetAddressUtility;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Network service
 *
 * @author Yuriy Movchan Date: 04/28/2016
 */
@ApplicationScoped
@Named
public class NetworkService implements Serializable {

    private static final long serialVersionUID = -1393318600428448743L;

    @Inject
    private Logger log;

    public String getRemoteIp() {
        String remoteIp = "";
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return remoteIp;
            }

            final HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

            remoteIp = request.getHeader("X-FORWARDED-FOR");
            if (StringHelper.isEmpty(remoteIp)) {
                remoteIp = request.getRemoteAddr();
            }

            return remoteIp;
        } catch (Exception ex) {
            log.error("Failed to get remote IP", ex);
        }

        return remoteIp;
    }

    public String getHost(String serverUri) {
        URI uri;
        try {
            uri = new URI(serverUri);
            return uri.getHost();
        } catch (URISyntaxException ex) {
            log.error("Failed to get remote IP", ex);
        }

        return serverUri;
    }

	public String getMacAdress() {
		return InetAddressUtility.getMACAddressOrNull();
	}

}
