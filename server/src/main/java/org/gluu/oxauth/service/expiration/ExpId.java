package org.gluu.oxauth.service.expiration;

import java.util.Objects;

/**
 * @author Yuriy Zabrovarnyy
 */
class ExpId {

    private String key;
    private ExpType type;

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
