/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.model;

//import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

public class AuthenticationMethod implements Serializable {

    private static final long serialVersionUID = 1L;

    //@NotNull
    @NotBlank
    @Size(min = 1)
    private String defaultAcr;

    public String getDefaultAcr() {
        return defaultAcr;
    }

    public void setDefaultAcr(String defaultAcr) {
        this.defaultAcr = defaultAcr;
    }

    @Override
    public String toString() {
        return "AuthenticationMethod [" + " defaultAcr=" + defaultAcr + "]";
    }

}
