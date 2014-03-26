package org.gluu.site.ldap.persistence.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * LDAP DN
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface LdapDN {
}
