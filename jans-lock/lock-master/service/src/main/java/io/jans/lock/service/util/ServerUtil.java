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

package io.jans.lock.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.CacheControl;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 26/12/2012
 */

public class ServerUtil {

	private static final Logger log = LoggerFactory.getLogger(ServerUtil.class);

	public static final String PRAGMA = "Pragma";
	public static final String NO_CACHE = "no-cache";

	public static CacheControl cacheControl(boolean noStore) {
		final CacheControl cacheControl = new CacheControl();
		cacheControl.setNoStore(noStore);
		return cacheControl;
	}

	public static CacheControl cacheControl(boolean noStore, boolean noTransform) {
		final CacheControl cacheControl = new CacheControl();
		cacheControl.setNoStore(noStore);
		cacheControl.setNoTransform(noTransform);
		return cacheControl;
	}

	public static CacheControl cacheControlWithNoStoreTransformAndPrivate() {
		final CacheControl cacheControl = cacheControl(true, false);
		cacheControl.setPrivate(true);
		return cacheControl;
	}

}
