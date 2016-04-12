/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class Configuration {

    private Map<String, String> pkcs11Config;
    private String pkcs11Pin;
    private String dnName;

    public Map<String, String> getPkcs11Config() {
        if (pkcs11Config == null) {
            pkcs11Config = new HashMap<String, String>();
        }

        return pkcs11Config;
    }

    public void setPkcs11Config(Map<String, String> pkcs11Config) {
        this.pkcs11Config = pkcs11Config;
    }

    public String getPkcs11Pin() {
        return pkcs11Pin;
    }

    public void setPkcs11Pin(String pkcs11Pin) {
        this.pkcs11Pin = pkcs11Pin;
    }

    public String getDnName() {
        return dnName;
    }

    public void setDnName(String dnName) {
        this.dnName = dnName;
    }
}
