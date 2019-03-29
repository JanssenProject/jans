package org.gluu.oxauth.model.auth;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;

/**
 * @author Yuriy Movchan
 * Date: 03/17/2017
 */
@Vetoed
public class AuthenticationMode implements Serializable {

	private static final long serialVersionUID = -3187893527945584013L;

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
