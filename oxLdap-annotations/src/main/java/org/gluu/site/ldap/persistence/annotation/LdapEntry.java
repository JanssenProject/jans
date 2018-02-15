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
 * Mark POJO class as LDAP entry
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface LdapEntry {

    /**
     * (Optional) Specify that this entry contains schema definition.
     */
    boolean schemaDefinition() default false;

    /**
     * (Optional) Specify sortBy properties to sort by default list of Entries.
     */
    String[] sortBy() default {};

}
