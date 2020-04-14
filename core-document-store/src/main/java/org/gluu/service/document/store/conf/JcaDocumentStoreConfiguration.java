package org.gluu.service.document.store.conf;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JcaDocumentStoreConfiguration implements Serializable {

    private static final long serialVersionUID = 3380170170265842427L;

    private String serverUrl; // http://localhost:8080/
    
    private String userName;
    
    private String userPassowrd;

    public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPassowrd() {
		return userPassowrd;
	}

	public void setUserPassowrd(String userPassowrd) {
		this.userPassowrd = userPassowrd;
	}

	@Override
	public String toString() {
		return "JCAConfiguration [serverUrl=" + serverUrl + ", userName=" + userName + ", userPassowrd=" + userPassowrd + "]";
	}
}
