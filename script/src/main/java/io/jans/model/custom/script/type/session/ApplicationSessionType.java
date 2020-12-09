/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.session;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external application session python script
 *
 * @author Yuriy Movchan Date: 12/30/2012
 */
public interface ApplicationSessionType extends BaseExternalType {

    boolean startSession(Object httpRequest, Object sessionState, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean endSession(Object httpRequest, Object sessionState, Map<String, SimpleCustomProperty> configurationAttributes);

    void onEvent(Object event);
}
