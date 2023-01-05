/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.model;

import jakarta.enterprise.inject.Vetoed;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version March 20, 2017
 */
@Vetoed
public class Configuration {

    private Map<String, String> pkcs11Config;
    private String pkcs11Pin;
    private String dnName;
    private String testModeToken;

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

    public String getTestModeToken() {
        return testModeToken;
    }

    public void setTestModeToken(String testModeToken) {
        this.testModeToken = testModeToken;
    }
}
