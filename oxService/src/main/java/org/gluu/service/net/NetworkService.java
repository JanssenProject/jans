/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service.net;

import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
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
}
