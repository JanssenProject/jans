/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.expiration;

import java.util.Objects;

/**
 * @author Yuriy Zabrovarnyy
 */
class ExpId {

    private final String key;
    private final ExpType type;

    public ExpId(String key, ExpType type) {
        this.key = key;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public ExpType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpId id = (ExpId) o;
        return Objects.equals(key, id.key) &&
                type == id.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type);
    }
}
