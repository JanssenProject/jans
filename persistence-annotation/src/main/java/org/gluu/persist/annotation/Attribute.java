/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persistence.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LDAP Attribute
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface LdapAttribute {

    /**
     * (Optional) The name of the LDAP attribute. Defaults to the field name.
     */
    String name() default "";

    /**
     * (Optional) Specify that we ignore this LDAP attribute during read.
     * Defaults value is false.
     */
    boolean ignoreDuringRead() default false;

    /**
     * (Optional) Specify that we ignore this LDAP attribute during update.
     * Defaults value is false.
     */
    boolean ignoreDuringUpdate() default false;

    /**
     * (Optional) Specify that we will only update this attribute, and never
     * remove it (set to null). Use this with health status attributes.
     */
    boolean updateOnly() default false;

}
