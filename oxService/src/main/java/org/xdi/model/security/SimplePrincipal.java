package org.xdi.model.security;

import java.security.Principal;

public class SimplePrincipal implements Principal {

	private String name;

	public SimplePrincipal(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}