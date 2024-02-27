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

	@DocProperty(description = "Enable Publish messages on access token issue/revoke")
    private Boolean enableTokenMessages;

	@DocProperty(description = "Channel for token messages")
    private String tokenMessagesChannel;


	public Boolean getEnableTokenMessages() {
		return enableTokenMessages;
	}

	public void setEnableTokenMessages(Boolean enableTokenMessages) {
		this.enableTokenMessages = enableTokenMessages;
	}

	public String getTokenMessagesChannel() {
		return tokenMessagesChannel;
	}

	public void setTokenMessagesChannel(String tokenMessagesChannel) {
		this.tokenMessagesChannel = tokenMessagesChannel;
	}

	@Override
	public String toString() {
		return "LockMessageConfig [enableTokenMessages=" + enableTokenMessages + ", tokenMessagesChannel="
				+ tokenMessagesChannel + "]";
	}
}
