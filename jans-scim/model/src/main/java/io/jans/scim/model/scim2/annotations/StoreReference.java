/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.annotations;

import io.jans.scim.model.scim2.BaseScimResource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to tie a field (class member) to an LDAP attribute. This is not used for persisting resources but
 * only for building LDAP filters when queries are issued. This is a mechanism to be able to convert filter expressions
 * as in section 3.4.2.2 of RFC 7644 to LDAP expressions.
 */
/*
 * Created by jgomer on 2017-10-10.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface StoreReference {

    /**
     * The LDAP attribute that the class field being annotated with this annotation ({@link StoreReference StoreReference})
     * is mapping to by default
     * @return LDAP attribute name
     */
    String ref() default "";

    /**
     * A collection of subclasses of the base class {@link io.jans.scim.model.scim2.BaseScimResource BaseScimResource}.
     * This "collection" goes paired with the values of element {@link #refs()}
     * @return An array of classes
     */
    Class<? extends BaseScimResource>[] resourceType() default {};

    /**
     * Describes a mapping of LDAP attribute names and resource types for the class field being annotated with
     * {@link StoreReference StoreReference}. This is useful when a default value for LDAP attribute cannot be given, that is,
     * when the LDAP attribute varies depending on whether it is a User or a Group
     * @return An array of strings (representing LDAP attribute names). When providing this element, also {@link #resourceType()}
     * must be provided and both arrays must have the same length
     */
    String[] refs() default {};

}
