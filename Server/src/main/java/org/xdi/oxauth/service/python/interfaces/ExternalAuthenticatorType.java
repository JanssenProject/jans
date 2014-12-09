/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.python.interfaces;

import org.xdi.oxauth.service.custom.interfaces.auth.CustomAuthenticatorType;

/**
 * Base interface for external authentication python script
 *
 * @author Yuriy Movchan Date: 08.21.2012
 */
@Deprecated
// Use org.xdi.oxauth.service.custom.interfaces.auth.CustomAuthenticatorType instead of this class
public interface ExternalAuthenticatorType extends CustomAuthenticatorType {
}
