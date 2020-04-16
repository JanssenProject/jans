package org.gluu.service.document.store.conf;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebDavDocumentStoreConfiguration implements Serializable {

	private static final long serialVersionUID = 3380170170265842538L;

	private String serverUrl; // http://localhost:8080
	private String workspaceName;
	private long connectionTimeout;

	private String userId;
	private String password;
	private String decryptedPassword;

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	public long getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDecryptedPassword() {
		return decryptedPassword;
	}

	public void setDecryptedPassword(String decryptedPassword) {
		this.decryptedPassword = decryptedPassword;
	}

	@Override
	public String toString() {
		return "WebDavDocumentStoreConfiguration [serverUrl=" + serverUrl + ", workspaceName=" + workspaceName + ", connectionTimeout="
				+ connectionTimeout + ", userId=" + userId + ", password=" + password + ", decryptedPassword=" + decryptedPassword + "]";
	}

}
