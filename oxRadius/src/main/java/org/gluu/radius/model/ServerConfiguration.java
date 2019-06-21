/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2019, Gluu
 */

package org.gluu.radius.model;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.ObjectClass;


@DataEntry
@ObjectClass(values={"oxRadiusServerConfiguration","top"})
public class ServerConfiguration implements Serializable {

	private static final long serialVersionUID = -1L;
	
	public static final String LISTEN_ON_ALL_INTERFACES = "0.0.0.0";
	public static final Integer DEFAULT_RADIUS_AUTH_PORT = 1812;
	public static final Integer DEFAULT_RADIUS_ACCT_PORT = 1813;

	@DN
	private String dn;
	
	@AttributeName(name="oxRadiusListenInterface")
	private String listenInterface;

	@AttributeName(name="oxRadiusAuthenticationPort")
	private Integer authPort;

	@AttributeName(name="oxRadiusAccountingPort")
	private Integer acctPort;

	@AttributeName(name="oxRadiusOpenidUsername")
	private String openidUsername;

	@AttributeName(name="oxRadiusOpenidPassword")
	private String openidPassword;

	@AttributeName(name="oxRadiusOpenIdBaseUrl")
	private String openidBaseUrl;

	@AttributeName(name="oxRadiusAcrValue")
	private String acrValue;

	@AttributeName(name="oxRadiusAuthScope")
	private List<String> scopes;

	@AttributeName(name="oxRadiusAuthenticationTimeout")
	private Integer authenticationTimeout;


	public ServerConfiguration() {

		this.dn = null;
		this.listenInterface = LISTEN_ON_ALL_INTERFACES;
		this.authPort = DEFAULT_RADIUS_AUTH_PORT;
		this.acctPort = DEFAULT_RADIUS_ACCT_PORT;
		this.scopes = new ArrayList<String>();
		this.authenticationTimeout = 0;
	}


	public ServerConfiguration(String listeninterface,Integer authport,Integer acctport,String openidusername, 
		String openidpassword) {

		this.listenInterface = listeninterface;
		this.authPort = authport;
		this.acctPort = acctport;
		this.openidUsername = openidusername;
		this.openidPassword = openidpassword;
		this.scopes = new ArrayList<String>();
		this.authenticationTimeout = 0;
	}


	public String getDn() {

		return this.dn;
	}

	public void setDn(String dn) {
		
		this.dn = dn;
	}

	public String getListenInterface() {

		return this.listenInterface;
	}

	public void setListenInterface(String listeninterface) {

		this.listenInterface = listeninterface;
	}

	public Integer getAuthPort() {

		return this.authPort;
	}

	public void setAuthPort(Integer authPort) {

		this.authPort = authPort;
	}

	public Integer getAcctPort() {

		return this.acctPort;
	}

	public void setAcctPort(Integer acctPort) {

		this.acctPort = acctPort;
	}

	public String getOpenidUsername() {

		return this.openidUsername;
	}

	public void setOpenidUsername(String openidusername) {

		this.openidUsername = openidusername;
	}


	public String getOpenidPassword() {

		return this.openidPassword;
	}


	public void setOpenidPassword(String openidpassword) {

		this.openidPassword = openidpassword;
	}

	public String getOpenidBaseUrl() {

		return this.openidBaseUrl;
	}

	public void setOpenidBaseUrl(String openidBaseUrl) {

		this.openidBaseUrl = openidBaseUrl;
	}

	public String getAcrValue() {

		return this.acrValue;
	}

	public void setAcrValue(String acrValue) {

		this.acrValue = acrValue;
	}

	public List<String> getScopes() {

		return this.scopes;
	}

	public void setScopes(List<String> scopes) {

		this.scopes = scopes;
	}

	public Integer getAuthenticationTimeout() {

		return this.authenticationTimeout;
	}

	public void setAuthenticationTimeout(Integer authenticationTimeout) {

		this.authenticationTimeout = authenticationTimeout;
	}


}