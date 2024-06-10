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

package io.jans.lock.service.external.context;

import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.token.TokenEntity;

/**
 * External lock context
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
public class ExternalLockContext {

    private CustomScriptConfiguration script;
    
    private TokenEntity tokenEntity;

    private boolean cancelPdpOperation;
    private boolean cancelNextScriptOperation;

    public ExternalLockContext() {
    	this.cancelNextScriptOperation = false;
    	this.cancelPdpOperation = false;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

	public TokenEntity getTokenEntity() {
		return tokenEntity;
	}

	public void setTokenEntity(TokenEntity tokenEntity) {
		this.tokenEntity = tokenEntity;
	}

	public boolean isCancelPdpOperation() {
		return cancelPdpOperation;
	}

	public void setCancelPdpOperation(boolean cancelPdpOperation) {
		this.cancelPdpOperation = cancelPdpOperation;
	}

	public boolean isCancelNextScriptOperation() {
		return cancelNextScriptOperation;
	}

	public void setCancelNextScriptOperation(boolean cancelNextScriptOperation) {
		this.cancelNextScriptOperation = cancelNextScriptOperation;
	}

	@Override
	public String toString() {
		return "ExternalLockContext [script=" + script + ", tokenEntity=" + tokenEntity + ", cancelPdpOperation="
				+ cancelPdpOperation + ", cancelNextScriptOperation=" + cancelNextScriptOperation + "]";
	}

}
