/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.net;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;
import org.xdi.util.StringHelper;

/**
 * Network service
 *
 * @author Yuriy Movchan Date: 04/28/2016
 */
public class NetworkService implements Serializable {

	private static final long serialVersionUID = -1393318600428448743L;

	@Logger
	private Log log;

    public String getRemoteIp() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }

            final HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

            String remoteIp = request.getHeader("X-FORWARDED-FOR");
            if (StringHelper.isEmpty(remoteIp)) {
            	remoteIp = request.getRemoteAddr();
            }
            
            return remoteIp;
        } catch (Exception ex) {
            log.error("Failed to get remote IP", ex);
        }
        
        return null;
    }

	/**
	 * Get NetworkService instance
	 *
	 * @return NetworkService instance
	 */
	public static NetworkService instance() {
		return (NetworkService) Component.getInstance(NetworkService.class);
	}

}
