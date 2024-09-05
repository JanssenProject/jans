/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.user.authenticator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.jans.model.user.authenticator.serialize.UserAuthenticatorDeserializer;
import io.jans.model.user.authenticator.serialize.UserAuthenticatorSerializer;

/**
 * User authenticators list
 *
 * @author Yuriy Movchan Date: 03/28/2024
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonSerialize(using = UserAuthenticatorSerializer.class)
@JsonDeserialize(using = UserAuthenticatorDeserializer.class)
public class UserAuthenticatorList implements Serializable {

	private static final long serialVersionUID = -8173244116167488365L;

	private List<UserAuthenticator> authenticators;

	public UserAuthenticatorList() {}

	public UserAuthenticatorList(List<UserAuthenticator> authenticators) {
		this.authenticators = authenticators;
	}

	public List<UserAuthenticator> getAuthenticators() {
		return authenticators;
	}

	public void setAuthenticators(List<UserAuthenticator> authenticators) {
		this.authenticators = authenticators;
	}

	public void addAuthenticator(UserAuthenticator newAuthenticator) {
		if (authenticators == null) {
			this.authenticators = new ArrayList<>();
		}
		this.authenticators.add(newAuthenticator);
	}

	@JsonIgnore
	public boolean isEmpty() {
		return (authenticators == null) || (authenticators.size() == 0);
	}

	@Override
	public String toString() {
		return "UserAuthenticatorList [authenticators=" + authenticators + "]";
	}

}
