/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation aimed at being used for service operations that do not require an Authorization header to be included.
 * Examples of this are {@link ClientSideService#getResourceTypes()} or {@link ClientSideService#getSchemas()}.
 * <p>See the implementation of method {@link io.jans.scim2.client.AbstractScimClient#invoke(Object, Method, Object[])}</p>
 */
/*
 * Created by jgomer on 2017-11-25.
 */
@Retention(RUNTIME)
@Target({METHOD})
public @interface FreelyAccessible {}
