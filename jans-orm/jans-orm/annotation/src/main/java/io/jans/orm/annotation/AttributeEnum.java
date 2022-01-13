/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.annotation;

/**
 * Base interface for Persistance enumerations
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public interface AttributeEnum {

    String getValue();

    Enum<? extends AttributeEnum> resolveByValue(String value);

}
