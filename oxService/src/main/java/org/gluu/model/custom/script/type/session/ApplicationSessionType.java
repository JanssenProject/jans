/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.session;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external application session python script
 *
 * @author Yuriy Movchan Date: 12/30/2012
 */
public interface ApplicationSessionType extends BaseExternalType {

    boolean startSession(Object httpRequest, Object sessionState, Map<String, SimpleCustomProperty> configurationAttributes);
    boolean endSession(Object httpRequest, Object sessionState, Map<String, SimpleCustomProperty> configurationAttributes);

}
