package org.xdi.oxauth.model.auth;

/**
 * @author Yuriy Movchan
 * Date: 03/17/2017
 */
public class AuthenticationMode {

	private String name;

	public AuthenticationMode() {}

	public AuthenticationMode(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
