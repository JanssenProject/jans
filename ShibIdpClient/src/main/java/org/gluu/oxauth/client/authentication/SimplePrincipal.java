package org.gluu.oxauth.client.authentication;

import java.io.Serializable;
import java.security.Principal;

import org.xdi.util.AssertionHelper;

/**
 * Simple security principal implementation
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public class SimplePrincipal implements Principal, Serializable {

	private static final long serialVersionUID = 7462821189097944893L;

	private final String name;

	public SimplePrincipal(final String name) {
		this.name = name;
		AssertionHelper.assertNotNull(this.name, "Name cannot be null!");
	}

	public final String getName() {
		return this.name;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimplePrincipal [name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimplePrincipal other = (SimplePrincipal) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
