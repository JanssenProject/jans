/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LDAP Attributes List
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface LdapAttributesList {

    /**
     * (Required) The class property name which contains LDAP attribute name.
     */
    String name();

    /**
     * (Required) The class property name which contains LDAP attribute value.
     */
    String value();

    /**
     * (Optional) Holds additional configuration for LDAP attributes. Defaults
     * value not provides additional configuration.
     */
    LdapAttribute[] attributesConfiguration() default {};

    /**
     * (Optional) Specify if attributes should be sorted by property name value.
     */
    boolean sortByName() default false;

}
