package org.xdi.config.oxtrust;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author Shekhar L.
 * @Date 07/17/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportConfiguration {
	
	private String provider;
	private String clientID;
	private String clientSecret;
	private String callbackURL;
	private String serverURI;
	private String serverWebPort;
	private String applicationEndpoint;
	private String applicationStartpoint;
	private String applicationSecretKey;
	
	
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getClientID() {
		return clientID;
	}
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	public String getCallbackURL() {
		return callbackURL;
	}
	public void setCallbackURL(String callbackURL) {
		this.callbackURL = callbackURL;
	}
	public String getServerURI() {
		return serverURI;
	}
	public void setServerURI(String serverURI) {
		this.serverURI = serverURI;
	}
	public String getServerWebPort() {
		return serverWebPort;
	}
	public void setServerWebPort(String serverWebPort) {
		this.serverWebPort = serverWebPort;
	}
	public String getApplicationEndpoint() {
		return applicationEndpoint;
	}
	public void setApplicationEndpoint(String applicationEndpoint) {
		this.applicationEndpoint = applicationEndpoint;
	}
	public String getApplicationStartpoint() {
		return applicationStartpoint;
	}
	public void setApplicationStartpoint(String applicationStartpoint) {
		this.applicationStartpoint = applicationStartpoint;
	}
	public String getApplicationSecretKey() {
		return applicationSecretKey;
	}
	public void setApplicationSecretKey(String applicationSecretKey) {
		this.applicationSecretKey = applicationSecretKey;
	}

}
