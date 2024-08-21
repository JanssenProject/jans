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

package io.jans.lock.service.message;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.service.message.consumer.MessageConsumer;
import io.jans.service.message.provider.MessageProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan Date: 15/12/2023
 */
@ApplicationScoped
public class TokenSubService {

    @Inject
    protected Logger log;

    @Inject
	private AppConfiguration appConfiguration;

    @Inject
	private MessageProvider messageProvider;

    @Inject
	private MessageConsumer messageConsumer;

	public void subscribe() {
		log.info("Use message provider type: {}", messageProvider.getProviderType());
		
		messageProvider.subscribe(messageConsumer, appConfiguration.getTokenChannels().toArray(new String[0]));

		log.info("Subscribed to channels: {}", appConfiguration.getTokenChannels());
	}
}
