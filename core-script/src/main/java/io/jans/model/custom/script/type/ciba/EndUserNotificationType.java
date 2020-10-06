/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
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
