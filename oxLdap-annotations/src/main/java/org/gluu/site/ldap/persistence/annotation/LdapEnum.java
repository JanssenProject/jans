package org.gluu.site.ldap.persistence.annotation;

/**
 * Base interface for LDAP enumerations
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
public interface LdapEnum {

	public String getValue();

	public Enum<? extends LdapEnum> resolveByValue(String value);

}
