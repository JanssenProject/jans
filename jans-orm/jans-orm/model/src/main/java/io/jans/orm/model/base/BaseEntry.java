/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides DN attribute
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class BaseEntry extends Entry {

    public BaseEntry() {
        super();
    }

    public BaseEntry(String dn) {
        super(dn);
    }

    @Override
    public String toString() {
        return String.format("BaseEntry [dn=%s]", getDn());
    }

    public static List<String> getDNs(Collection<? extends BaseEntry> collection) {
        return collection.stream().map(BaseEntry::getDn).collect(Collectors.toList());
    }

}
