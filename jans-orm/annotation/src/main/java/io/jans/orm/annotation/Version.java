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
 * Marks a persisted attribute as the entry's optimistic-concurrency version. The annotated field
 * must also carry {@link AttributeName} for its column/attribute name. Exactly one
 * {@code @Version} field is allowed per entity class.
 *
 * @author Yuriy Movchan Date: 10.02.2026
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
}
