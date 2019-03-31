package org.gluu.oxd.server.op;

import java.util.Objects;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JOb {

    String name;
    int id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JOb jOb = (JOb) o;
        return id == jOb.id && Objects.equals(name, jOb.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
