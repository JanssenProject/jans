/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version April 13, 2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "client-auth-filter")
public class ClientAuthenticationFilter extends BaseFilter {
}
