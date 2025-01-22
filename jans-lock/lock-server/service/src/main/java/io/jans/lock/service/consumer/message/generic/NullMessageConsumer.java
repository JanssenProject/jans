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

package io.jans.lock.service.consumer.message.generic;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.qualifier.Implementation;
import io.jans.service.message.consumer.MessageConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Null message consumer
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@Implementation
@ApplicationScoped
public class NullMessageConsumer extends MessageConsumer {
	
	public static String MESSAGE_CONSUMER_TYPE = "DISABLED";

	@Inject
	private Logger log;

	/*
	 * Message: {"tknTyp" : "access_token", "tknId": "UUID"}
	 */
	@Override
	@Asynchronous
	public void onMessage(String channel, String message) {
		log.info("onMessage {} : {} : {}", channel, message);
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		log.debug("onSubscribe {} : {}", channel, subscribedChannels);
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		log.debug("onUnsubscribe {} : {}", channel, subscribedChannels);
	}

	@Override
	public String getMessageConsumerType() {
		return MESSAGE_CONSUMER_TYPE;
	}

	@Override
	public void destroy() {
		log.debug("Destroy Messages");
	}

	@Override
	public boolean putData(String message, JsonNode messageNode) {
		return false;
	}

}
