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

package io.jans.lock.service.ws.rs.sse;

import org.slf4j.Logger;

import io.jans.lock.service.ws.rs.base.BaseResource;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * @author Yuriy Movchan Date: 05/24/2024
 */
@Dependent
@Path("/")
public class SseRestWebServiceImpl extends BaseResource implements SseRestWebService {

	@Inject
	private Logger log;
    
    @Inject
    private LockSseBroadcaster lockSseBroadcater;

	@Override
	public void subscribe(@Context Sse sse, @Context SseEventSink sseEventSink) {
        log.info("Subscribe broadcaster");
        if (lockSseBroadcater.getSseBroadcaster() == null) {
            log.info("Init broadcaster");
            lockSseBroadcater.setSse(sse);
        	lockSseBroadcater.setSseBroadcaster(sse.newBroadcaster());
        }

        lockSseBroadcater.getSseBroadcaster().register(sseEventSink);
	}

}
