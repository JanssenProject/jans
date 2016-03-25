/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.tomcat7.jsee;

import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.tomcat.util.net.SSLSupport;

/**
 * Allows to want client certificate instead of request during re-handshake
 * 
 * @author Yuriy Movchan Date: 02/11/2016
 */
public class WantCertJSSESupport implements SSLSupport {

	private static final String TOMCAT_JSSE_SUPPORT = "org.apache.tomcat.util.net.jsse.JSSESupport";

	private static final org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory.getLog(WantCertJSSEImplementation.class);

	protected SSLSocket ssl;

	private SSLSupport jseeSupport;

	public WantCertJSSESupport(SSLSocket sock) {
    	this.ssl = sock;

    	try {
            Class<?> clazz = Class.forName(TOMCAT_JSSE_SUPPORT);
            Constructor<?> cons = clazz.getDeclaredConstructor(SSLSocket.class);
            cons.setAccessible(true);
            this.jseeSupport = (SSLSupport) cons.newInstance(sock);
		} catch (Exception ex) {ex.printStackTrace();
			throw new RuntimeException("Error creating  object " + TOMCAT_JSSE_SUPPORT);
			
		}
	}

	public WantCertJSSESupport(SSLSession session) {
    	try {
            Class<?> clazz = Class.forName(TOMCAT_JSSE_SUPPORT);
            Constructor<?> cons = clazz.getDeclaredConstructor(SSLSession.class);
            cons.setAccessible(true);
            this.jseeSupport = (SSLSupport) cons.newInstance(session);
		} catch (Exception ex) {
			throw new RuntimeException("Error creating  object " + TOMCAT_JSSE_SUPPORT);
		}
	}

	@Override
	public String getCipherSuite() throws IOException {
		return this.jseeSupport.getCipherSuite();
	}

	@Override
	public Object[] getPeerCertificateChain() throws IOException {
		return this.jseeSupport.getPeerCertificateChain();
	}

	@Override
	public Object[] getPeerCertificateChain(boolean force) throws IOException {
		if (!ssl.isClosed()) {
			// Just want certificate
			this.ssl.setWantClientAuth(true);
		}
        
		Object[] result = null;
		try {
			result =  this.jseeSupport.getPeerCertificateChain(force);
		} catch (java.net.SocketException ex) {
			if (log.isDebugEnabled()) {
				// TODO: Review this part later
				log.error("There is no certs. This issue not exist till OpenJDK 1.7.0_79 or Oracle JDK 1.6.0_37.", ex);
			}
		}
		
		return result;
	}

	@Override
	public Integer getKeySize() throws IOException {
		return this.jseeSupport.getKeySize();
	}

	@Override
	public String getSessionId() throws IOException {
		return this.jseeSupport.getSessionId();
	}

	@Override
	public String getProtocol() throws IOException {
		return this.jseeSupport.getProtocol();
	}

}
