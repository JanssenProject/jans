package org.gluu.service.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface SecureVariable {

    /**
     * @return Name of the variable
     */
    String value();

}
