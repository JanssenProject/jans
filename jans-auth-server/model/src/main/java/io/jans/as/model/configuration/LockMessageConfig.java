package io.jans.as.model.configuration;

import java.io.Serializable;

import io.jans.doc.annotation.DocProperty;

/**
 * 
 * Lock message Pub configuration
 *
 * @author Yuriy Movchan Date: 12/31/2023
 *
 */
public class LockMessageConfig implements Serializable {

	private static final long serialVersionUID = 8732855593629219229L;

	@DocProperty(description = "Enable Publish messages on id_token issue/revoke")
    private Boolean enableIdTokenMessages;

	@DocProperty(description = "Channel for id_token messages")
    private String idTokenMessagesChannel;

	
    public Boolean getEnableIdTokenMessages() {
		return enableIdTokenMessages;
	}

	public void setEnableIdTokenMessages(Boolean enableIdTokenMessages) {
		this.enableIdTokenMessages = enableIdTokenMessages;
	}

	public String getIdTokenMessagesChannel() {
		return idTokenMessagesChannel;
	}

	public void setIdTokenMessagesChannel(String idTokenMessagesChannel) {
		this.idTokenMessagesChannel = idTokenMessagesChannel;
	}

	@Override
	public String toString() {
		return "LockMessageConfig [enableIdTokenMessages=" + enableIdTokenMessages + ", idTokenMessagesChannel="
				+ idTokenMessagesChannel + "]";
	}
}
