/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Hold SMTP configuration
 * 
 * @author Yuriy Movchan Date: 04/20/2014
 */
@XmlRootElement
public class SmtpConfiguration implements java.io.Serializable {

	private static final long serialVersionUID = -5675038049444038755L;

    @XmlElement(name = "host")
	private String host;
    @XmlElement(name = "port")
	private int port;
    @XmlElement(name = "requires-ssl")
	private boolean requiresSsl;

    @XmlElement(name = "from-name")
	private String fromName;
    @XmlElement(name = "from-email-address")
	private String fromEmailAddress;

    @XmlElement(name = "requires-authentication")
	private boolean requiresAuthentication;

    @XmlElement(name = "user-name")
	private String userName;
    @XmlElement(name = "password")
	private String password;

	@Transient
    @JsonIgnore
	private String passwordDecrypted;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isRequiresSsl() {
		return requiresSsl;
	}

	public void setRequiresSsl(boolean requiresSsl) {
		this.requiresSsl = requiresSsl;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getFromEmailAddress() {
		return fromEmailAddress;
	}

	public void setFromEmailAddress(String fromEmailAddress) {
		this.fromEmailAddress = fromEmailAddress;
	}

	public boolean isRequiresAuthentication() {
		return requiresAuthentication;
	}

	public void setRequiresAuthentication(boolean requiresAuthentication) {
		this.requiresAuthentication = requiresAuthentication;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordDecrypted() {
		return passwordDecrypted;
	}

	public void setPasswordDecrypted(String passwordDecrypted) {
		this.passwordDecrypted = passwordDecrypted;
	}

}
