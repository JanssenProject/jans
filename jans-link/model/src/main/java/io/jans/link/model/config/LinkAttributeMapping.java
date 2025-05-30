/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.link.model.config;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.enterprise.inject.Vetoed;


/**
 * Attribute mapping
 *
 * @author Yuriy Movchan Date: 10/23/2015
 */
@Vetoed
@JsonPropertyOrder({ "source", "destination" })
public class LinkAttributeMapping extends io.jans.link.model.config.shared.LinkAttributeMapping implements Serializable {

}
