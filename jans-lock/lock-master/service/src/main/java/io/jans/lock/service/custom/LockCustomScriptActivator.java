/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.service.custom;

import java.util.Arrays;
import java.util.List;

import io.jans.model.custom.script.CustomScriptType;
import io.jans.service.custom.script.CustomScriptActivator;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Provides lock service plugin list of lock scripts
 *
 * @author Yuriy Movchan Date: 12/22/2023
 */
@ApplicationScoped
public class LockCustomScriptActivator implements CustomScriptActivator {

	@Override
	public List<CustomScriptType> getActiveCustomScripts() {
		return Arrays.asList(CustomScriptType.LOCK_EXTENSION);
	}
	
}
