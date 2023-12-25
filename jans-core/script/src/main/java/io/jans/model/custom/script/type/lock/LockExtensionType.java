/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.model.custom.script.type.lock;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external lock python script
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
public interface LockExtensionType extends BaseExternalType {
	
	void beforeDataPut(Object context, Object jsonData);

	void beforePolicyPut(Object context, Object jsonPolicy);

}
