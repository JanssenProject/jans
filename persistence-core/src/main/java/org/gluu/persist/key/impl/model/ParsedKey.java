/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.persist.key.impl.model;

/**
 * Couchbase key with inum
 *
 * @author Yuriy Movchan Date: 05/31/2018
 */
public class ParsedKey {

    private final String key;
    private final String name;
    private final String inum;

    public ParsedKey(final String key, final String name, String inum) {
        this.key = key;
        this.name = name;
        this.inum = inum;
    }

    public final String getKey() {
        return key;
    }

    public String getName() {
		return name;
	}

	public final String getInum() {
        return inum;
    }

}
