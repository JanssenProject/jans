package org.gluu.model.security;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * Represents a remember me flag
 */
@RequestScoped
@Named
public class RememberMe implements Serializable {
    private static final long serialVersionUID = 7783531488543645695L;

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
