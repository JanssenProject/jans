/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.model.scim2.annotations;

import org.gluu.oxtrust.model.scim2.Validations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation employed to associate a property of a SCIM resource with a concrete validation that should be applied on
 * it. See enumeration class {@link org.gluu.oxtrust.model.scim2.Validations Validations} for the existing validation types
 * applicable to SCIM attributes/subattributes.
 */
/*
 * Created by jgomer on 2017-09-15.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Validator {

    /**
     * Specifies a {@link org.gluu.oxtrust.model.scim2.Validations Validations} object
     * @return Validation object
     */
    Validations value();

}
