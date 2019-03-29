package org.gluu.model.security;

import java.io.Serializable;
import java.security.Principal;

public class SimplePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1129298490936584992L;

    private String name;

    public SimplePrincipal(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Principal) {
            Principal other = (Principal) obj;
            return name == null ? other.getName() == null : name.equals(other.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}
