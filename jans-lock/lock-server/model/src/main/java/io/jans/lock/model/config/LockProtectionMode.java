/*
 * Copyright [2025] [Janssen Project]
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

package io.jans.lock.model.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
public enum LockProtectionMode {

	OAUTH("oauth"), CEDARLING("cedarling");

	private final String mode;

	/**
     * Creates a LockProtectionMode with the given string representation.
     *
     * @param mode the string value used to represent this enum constant (for JSON serialization)
     */
    private LockProtectionMode(String mode) {
        this.mode = mode;
    }

	/**
	 * The value used when this enum is serialized to JSON.
	 *
	 * @return the enum constant's mode string, e.g. "oauth" or "cedarling"
	 */
	@JsonValue
	public String getMode() {
		return mode;
	}
}