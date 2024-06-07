/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
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
