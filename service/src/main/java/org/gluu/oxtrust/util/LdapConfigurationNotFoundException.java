package org.gluu.oxtrust.util;

import org.apache.commons.lang3.StringUtils;

public class LdapConfigurationNotFoundException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2331216889853939688L;
	private String name;

    public LdapConfigurationNotFoundException(String name) {
        this.name = name;
    }

    public LdapConfigurationNotFoundException() {
        this(StringUtils.EMPTY);
    }

    public String getName() {
        return name;
    }
}