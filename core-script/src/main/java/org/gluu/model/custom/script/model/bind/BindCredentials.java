/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.custom.script.model.bind;

import java.io.Serializable;

/**
 * Custom script configuration
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
public class BindCredentials implements Serializable {

    private static final long serialVersionUID = -953533543357212895L;

    private String bindDn;
    private String bindPassword;

    public BindCredentials() {}

    public BindCredentials(String bindDn, String bindPassword) {
        this.bindDn = bindDn;
        this.bindPassword = bindPassword;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

}
