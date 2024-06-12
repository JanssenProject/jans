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

package io.jans.lock.server.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import io.jans.lock.model.config.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Logger service
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
public class LoggerService extends io.jans.service.logger.LoggerService {

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public boolean isDisableJdkLogger() {
        return isTrue(appConfiguration.getDisableJdkLogger());
    }

    @Override
    public String getLoggingLevel() {
        return appConfiguration.getLoggingLevel();
    }

    @Override
    public String getExternalLoggerConfiguration() {
        return appConfiguration.getExternalLoggerConfiguration();
    }

    @Override
    public String getLoggingLayout() {
        return appConfiguration.getLoggingLayout();
    }

}
