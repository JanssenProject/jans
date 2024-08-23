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
    public static final String USER_DEVICE  = "/userdevice";
    
    public static final String USERNAME_PATH = "/{username}";
    public static final String UUID_PATH  = "/{uuid}";
    
    public static final String JANSID = "jansId";
    
    public static final String FIDO2_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/fido2.readonly";
    public static final String FIDO2_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/fido2.write";
    public static final String FIDO2_CONFIG_DELETE_ACCESS = "https://jans.io/oauth/config/fido2.delete";
}