/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.util;

public class Constants {

    private Constants() {}

    public static final String FIDO2_CONFIG = "/fido2-config";
    public static final String REGISTRATION = "/registration";
    public static final String ENTRIES = "/entries";
    public static final String DEVICE  = "/device";
    
    public static final String UID_PATH  = "{uid}";
    
    
    public static final String FIDO2_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/fido2.readonly";
    public static final String FIDO2_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/fido2.write";
    
    public static final String FIDO2_REGISTRATION_READ_ACCESS  = "https://jans.io/oauth/config/fido2/registration-read";
    public static final String FIDO2_REGISTRATION_WRITE_ACCESS = "https://jans.io/oauth/config/fido2/registration-write";
    public static final String FIDO2_REGISTRATION_DELETE_ACCESS = "https://jans.io/oauth/config/fido2/registration-delete";
}