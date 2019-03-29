/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents the authentication filter.
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version April 13, 2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auth-filter")
public class AuthenticationFilter extends BaseFilter {
}
