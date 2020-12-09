/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.user;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

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
