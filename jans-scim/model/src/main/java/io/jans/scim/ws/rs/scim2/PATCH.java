/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.HttpMethod;

/**
 * An annotation to be used by methods that handle HTTP requests using the PATCH verb.
 * @author Reda Zerrad Date: 04.18.2012
 */

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH")
public @interface PATCH { }
