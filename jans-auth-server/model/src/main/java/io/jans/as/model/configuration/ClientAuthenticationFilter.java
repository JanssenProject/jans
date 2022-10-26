/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version April 13, 2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "client-auth-filter")
public class ClientAuthenticationFilter extends BaseFilter {
}
