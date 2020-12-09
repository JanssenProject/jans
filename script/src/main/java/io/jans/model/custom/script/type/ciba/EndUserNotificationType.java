/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.ciba;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external authentication python script
 *
 * @author Milton BO Date: 04/22/2020
 */
public interface EndUserNotificationType extends BaseExternalType {

    boolean notifyEndUser(Object context);

}
