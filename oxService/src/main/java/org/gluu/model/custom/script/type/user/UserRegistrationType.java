/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.user;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external user registration python script
 *
 * @author Yuriy Movchan Date: 01/16/2015
 */
public interface UserRegistrationType extends BaseExternalType {

    boolean initRegistration(Object user, Map<String, String[]> requestParameters, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean preRegistration(Object user, Map<String, String[]> requestParameters, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postRegistration(Object user, Map<String, String[]> requestParameters, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean confirmRegistration(Object user, Map<String, String[]> requestParameters,
            Map<String, SimpleCustomProperty> configurationAttributes);

}
