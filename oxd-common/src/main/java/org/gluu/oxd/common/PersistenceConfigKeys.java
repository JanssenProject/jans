package org.gluu.oxd.common;

public enum PersistenceConfigKeys {

    PersistenceType("persistence.type"),
    BindPassword("bindPassword"),
    EncodeSalt("encodeSalt");

    private String keyName;

    private PersistenceConfigKeys(String keyName) {

        this.keyName = keyName;
    }

    public String getKeyName() {

        return this.keyName;
    }
}
