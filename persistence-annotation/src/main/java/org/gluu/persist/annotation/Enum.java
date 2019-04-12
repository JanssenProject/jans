/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.annotation;

/**
 * Base interface for LDAP enumerations
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public interface Enum {

    String getValue();

    Enum<? extends Enum> resolveByValue(String value);

}
