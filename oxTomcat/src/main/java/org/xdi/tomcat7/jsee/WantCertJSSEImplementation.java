/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.tomcat7.jsee;

import java.net.Socket;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SSLUtil;
import org.apache.tomcat.util.net.ServerSocketFactory;
import org.apache.tomcat.util.net.jsse.JSSEImplementation;
import org.apache.tomcat.util.net.jsse.JSSESocketFactory;

/**
 * Combine 2 implementations in one class to allow control re-handshake and delegate to application JSEE support
 * 
 * @author Yuriy Movchan Date: 02/11/2016
 */
public class WantCertJSSEImplementation extends JSSEImplementation {

	private static final String DELEGATE_TO_APPLICATION_JSSE_IMPLEMENTATION = "edu.internet2.middleware.security.tomcat7.DelegateToApplicationJSSEImplementation";

	private static final org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory.getLog(WantCertJSSEImplementation.class);

    private SSLImplementation delegateToApplicationJSSEImplementation = null;

	public WantCertJSSEImplementation() throws ClassNotFoundException {
    	try {
    		this.delegateToApplicationJSSEImplementation = getInstance(DELEGATE_TO_APPLICATION_JSSE_IMPLEMENTATION);
		} catch (ClassNotFoundException ex) {
			if (log.isErrorEnabled() || log.isDebugEnabled()) {
				log.error("Failed to load " + DELEGATE_TO_APPLICATION_JSSE_IMPLEMENTATION, ex);
			}
		}
	}

	@Override
    public String getImplementationName() {
		String suffix = "-WantCertJSSEImplementation";

		if (delegateToApplicationJSSEImplementation != null) {
	        return delegateToApplicationJSSEImplementation.getImplementationName() + suffix;
		}

		return super.getImplementationName() + suffix;
    }

    @Override
    public ServerSocketFactory getServerSocketFactory(AbstractEndpoint<?> endpoint)  {
    	if (delegateToApplicationJSSEImplementation != null) {
            return delegateToApplicationJSSEImplementation.getServerSocketFactory(endpoint);
    	}

    	return super.getServerSocketFactory(endpoint);
    }

    @Override
    public SSLSupport getSSLSupport(Socket sock) {
        return new WantCertJSSESupport((SSLSocket) sock);
    }

    @Override
    public SSLSupport getSSLSupport(SSLSession session) {
        return new WantCertJSSESupport(session);
    }

    @Override
    public SSLUtil getSSLUtil(AbstractEndpoint<?> endpoint) {
        return new JSSESocketFactory(endpoint);
    }
}
