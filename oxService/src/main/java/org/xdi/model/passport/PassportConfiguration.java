package org.xdi.model.passport;

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

}
