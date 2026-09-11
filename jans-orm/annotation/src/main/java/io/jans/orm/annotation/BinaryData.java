/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks byte[] entry property which should be stored in native binary column
 * without base64 encoding when DB supports binary types. If DB column type is
 * not binary the value is stored as base64 encoded string.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BinaryData {
}
