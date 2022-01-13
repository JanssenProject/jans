/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl.model;

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
