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

package io.jans.lock.service.external;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.external.context.ExternalLockContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.lock.LockExtensionType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * External lock service
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@ApplicationScoped
public class ExternalLockService extends ExternalScriptService {

    public ExternalLockService() {
        super(CustomScriptType.LOCK_EXTENSION);
    }

    public ExternalLockContext beforeDataPut(JsonNode messageNode, JsonNode dataNode, ExternalLockContext context) {
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!beforeDataPut(messageNode, dataNode, script, context)) {
                break;
            }
        }

        log.debug("Executed (beforeDataPut) from external lock script");
        return context;
    }

    public ExternalLockContext beforeDataRemoval(JsonNode messageNode, ExternalLockContext context) {
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!beforeDataRemoval(messageNode, script, context)) {
                break;
            }
        }

        log.debug("Executed (beforeDataRemoval) from external lock script");
        return context;
    }

    public ExternalLockContext beforePolicyPut(String sourceUri, List<String> policies, ExternalLockContext context) {
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!beforePolicyPut(sourceUri, policies, script, context)) {
                break;
            }
        }

        log.debug("Executed (beforePolicyPut) from external lock script");
        return context;
    }

    public ExternalLockContext beforePolicyRemoval(String sourceUri, ExternalLockContext context) {
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!beforePolicyRemoval(sourceUri, script, context)) {
                break;
            }
        }

        log.debug("Executed (beforePolicyRemoval) from external lock script");
        return context;
    }

    private boolean beforeDataPut(JsonNode messageNode, JsonNode dataNode, CustomScriptConfiguration scriptConfiguration, ExternalLockContext context) {
        try {
            log.trace("Executing external 'beforeDataPut' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            LockExtensionType script = (LockExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            script.beforeDataPut(messageNode, dataNode, context);
            return context.isCancelNextScriptOperation();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean beforeDataRemoval(JsonNode messageNode, CustomScriptConfiguration scriptConfiguration, ExternalLockContext context) {
        try {
            log.trace("Executing external 'beforeDataRemoval' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            LockExtensionType script = (LockExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            script.beforeDataRemoval(messageNode, context);
            return context.isCancelNextScriptOperation();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean beforePolicyPut(String sourceUri, List<String> policies, CustomScriptConfiguration scriptConfiguration, ExternalLockContext context) {
        try {
            log.trace("Executing external 'beforePolicyPut' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            LockExtensionType script = (LockExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            script.beforePolicyPut(sourceUri, policies, context);
            return context.isCancelNextScriptOperation();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean beforePolicyRemoval(String sourceUri, CustomScriptConfiguration scriptConfiguration, ExternalLockContext context) {
        try {
            log.trace("Executing external 'beforePolicyRemoval' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            LockExtensionType script = (LockExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            script.beforePolicyRemoval(sourceUri, context);
            return context.isCancelNextScriptOperation();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

}
