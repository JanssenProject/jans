/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.security;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider installation utility
 *
 * @author Yuriy Movchan
 * @author madhumitas
 * @author Sergey Manoylo
 * @version December 8, 2022
 */
public class SecurityProviderUtility {

    // security mode
    public static final String DEF_MODE_BCPROV      = "BCPROV";
    public static final String DEF_MODE_BCFIPS      = "BCFIPS";

    // keystorage type
    public static final String DEF_KS_JKS           = "JKS";
    public static final String DEF_KS_PKCS12        = "PKCS12";
    public static final String DEF_KS_BCFKS         = "BCFKS";

    // JKS additional extensions
    public static final String DEF_EXT_JKS          = "jks";
    public static final String DEF_EXT_KEYSTORE     = "keystore";
    public static final String DEF_EXT_KS           = "ks";

    // PKCS12 additional extensions
    public static final String DEF_EXT_PKCS12       = "pkcs12";
    public static final String DEF_EXT_P12          = "p12";
    public static final String DEF_EXT_PFX          = "pfx";

    // BCFKS additional extensions
    public static final String DEF_EXT_BCFKS        = "bcfks";
    public static final String DEF_EXT_BCF          = "bcf";
    public static final String DEF_EXT_BCFIPS       = "bcfips";

    /**
     * Security Mode Type
     * 
     * @author Sergey Manoylo
     * @version December 8, 2022 
     */
    public static enum SecurityModeType {

        BCPROV_SECURITY_MODE (DEF_MODE_BCPROV),
        BCFIPS_SECURITY_MODE (DEF_MODE_BCFIPS);

        private final String value;

        /**
         * Constructor
         * 
         * @param value string value, that defines Security Mode Type 
         */
        SecurityModeType(String value) {
            this.value = value;
        }

        /**
         * Creates/parses SecurityModeType from String value
         * 
         * @param param string value, that defines Security Mode Type
         * @return SecurityModeType
         */
        public static SecurityModeType fromString(String param) {
            switch(param.toUpperCase()) {
            case DEF_MODE_BCPROV: {
                return BCPROV_SECURITY_MODE;
            }
            case DEF_MODE_BCFIPS: {
                return BCFIPS_SECURITY_MODE;
            }
            }
            return null;
        }

        /**
         * Returns a string representation of the object. In this case the parameter name for the default scope.
         */
        @Override
        public String toString() {
            return value;
        }

        /**
         * 
         * @return
         */
        public KeyStorageType[] getKeystorageTypes() {
            KeyStorageType [] keystorages = null;
            if (this == BCPROV_SECURITY_MODE) {
                keystorages = new KeyStorageType[] { KeyStorageType.JKS_KS, KeyStorageType.PKCS12_KS };
            }
            else if (this == BCFIPS_SECURITY_MODE) {
                keystorages = new KeyStorageType[] { KeyStorageType.BCFKS_KS };
            }
            return keystorages;
        }
    }

    /**
     * Security Mode Type
     * 
     * @author Sergey Manoylo
     * @version December 8, 2022
     */
    public static enum KeyStorageType {

        JKS_KS (DEF_KS_JKS),
        PKCS12_KS (DEF_KS_PKCS12),
        BCFKS_KS (DEF_KS_BCFKS);

        private final String value;

        /**
         * Constructor
         * 
         * @param value string value, that defines Security Mode Type 
         */
        KeyStorageType(String value) {
            this.value = value;
        }

        /**
         * Creates/parses SecurityModeType from String value
         * 
         * @param param string value, that defines Security Mode Type
         * @return SecurityModeType
         */
        public static KeyStorageType fromString(String param) {
            switch(param.toUpperCase()) {
            case DEF_KS_JKS: {
                return JKS_KS;
            }
            case DEF_KS_PKCS12: {
                return PKCS12_KS;
            }
            case DEF_KS_BCFKS: {
                return BCFKS_KS;
            }
            }
            return null;
        }

        /**
         * Returns a string representation of the object. In this case the parameter name for the default scope.
         */
        @Override
        public String toString() {
            return value;
        }

        /**
         * 
         * @return
         */
        public String[] getExtensions() {
            String[] extensions = null;
            if (this == JKS_KS) {
                extensions = new String[] { DEF_EXT_JKS, DEF_EXT_KEYSTORE, DEF_EXT_KS };
            }
            else if(this == PKCS12_KS) {
                extensions = new String[] { DEF_EXT_PKCS12, DEF_EXT_P12, DEF_EXT_P12 };
            }
            else if(this == BCFKS_KS) {
                extensions = new String[] { DEF_EXT_BCFKS, DEF_EXT_BCF, DEF_EXT_BCFIPS };
            }
            return extensions;
        }

        /**
         * 
         * @return
         */
        public SecurityModeType getSecurityMode() {
            SecurityModeType securityModeType = null;
            if (this == JKS_KS || this == PKCS12_KS) {
                securityModeType = SecurityModeType.BCPROV_SECURITY_MODE;
            }
            else if(this == BCFKS_KS) {
                securityModeType =  SecurityModeType.BCFIPS_SECURITY_MODE;
            }
            return securityModeType;
        }

        /**
         * 
         * @param extension
         * @return
         */
        public static KeyStorageType fromExtension(String extension) {
            switch(extension.toLowerCase()) {
            case DEF_EXT_JKS:
            case DEF_EXT_KEYSTORE:
            case DEF_EXT_KS: {
                return JKS_KS;
            }
            case DEF_EXT_PKCS12:
            case DEF_EXT_P12:
            case DEF_EXT_PFX: {
                return PKCS12_KS;
            }
            case DEF_EXT_BCFKS:
            case DEF_EXT_BCF:
            case DEF_EXT_BCFIPS: {
                return BCFKS_KS;
            }
            }
            return null;
        }
    }    

    private static final Logger LOG = LoggerFactory.getLogger(SecurityProviderUtility.class);

    public static final String BC_PROVIDER_NAME = "BC";
    public static final String BC_FIPS_PROVIDER_NAME = "BCFIPS";

    public static boolean USE_FIPS_CHECK_COMMAND = false;

    private static SecurityModeType securityMode = null;

    private static Provider bouncyCastleProvider;

    private static final String BC_GENERIC_PROVIDER_CLASS_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    private static final String BC_FIPS_PROVIDER_CLASS_NAME    = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";

    public static void installBCProvider(boolean silent) {
        String providerName = BC_PROVIDER_NAME;
        String className = BC_GENERIC_PROVIDER_CLASS_NAME;

        if (securityMode == null || securityMode == SecurityModeType.BCFIPS_SECURITY_MODE) {
            boolean isFipsMode = checkFipsMode();
            if (isFipsMode) {
                LOG.info("Fips mode is enabled");

                providerName = BC_FIPS_PROVIDER_NAME;
                className = BC_FIPS_PROVIDER_CLASS_NAME;

                securityMode = SecurityModeType.BCFIPS_SECURITY_MODE;
            }
            else {
                securityMode = SecurityModeType.BCPROV_SECURITY_MODE;
            }
        }

        try {
            installBCProvider(providerName, className, silent);
        } catch (Exception e) {
            LOG.error(
                    "Security provider '{}' doesn't exists in class path. Please deploy correct war for this environment!", providerName);
            LOG.error(e.getMessage(), e);
        }
    }

    public static void installBCProvider() {
        installBCProvider(false);
    }

    public static void installBCProvider(String providerName, String providerClassName, boolean silent) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        bouncyCastleProvider = Security.getProvider(providerName);
        if (bouncyCastleProvider == null) {
            if (!silent) {
                LOG.info("Adding Bouncy Castle Provider");
            }

            bouncyCastleProvider = (Provider) Class.forName(providerClassName).getConstructor().newInstance();
            Security.addProvider(bouncyCastleProvider);
            LOG.info("Provider '{}' with version {} is added", bouncyCastleProvider.getName(), bouncyCastleProvider.getVersionStr());
        } else {
            if (!silent) {
                LOG.info("Bouncy Castle Provider was added already");
            }
        }
    }

    /**
    * A check that the server is running in FIPS-approved-only mode. This is a part
    * of compliance to ensure that the server is really FIPS compliant
    * 
    * @return boolean value
    */
    private static boolean checkFipsMode() {
        try {
            // First check if there are FIPS provider libs
            Class.forName(BC_FIPS_PROVIDER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            LOG.trace("BC Fips provider is not available", e);
            return false;
        }
        return true;
    }

    /**
     * Determines if cryptography restrictions apply.
     * Restrictions apply if the value of {@link Cipher#getMaxAllowedKeyLength(String)} returns a value smaller than {@link Integer#MAX_VALUE} if there are any restrictions according to the JavaDoc of the method.
     * This method is used with the transform <code>"AES/CBC/PKCS5Padding"</code> as this is an often used algorithm that is <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#impl">an implementation requirement for Java SE</a>.
     *
     * @return <code>true</code> if restrictions apply, <code>false</code> otherwise
     * https://stackoverflow.com/posts/33849265/edit, author Maarten Bodewes
     */
    public static boolean checkRestrictedCryptography() {
        try {
            return Cipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding") < Integer.MAX_VALUE;
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("The transform \"AES/CBC/PKCS5Padding\" is not available (the availability of this algorithm is mandatory for Java SE implementations)", e);
        }
    }

    public static String getBCProviderName() {
        return bouncyCastleProvider.getName();
    }

    public static Provider getBCProvider() {
        return bouncyCastleProvider;
    }

    public static SecurityModeType getSecurityMode() {
        return securityMode;
    }

    public static void setSecurityMode(SecurityModeType securityModeIn) {
        securityMode = securityModeIn;
    }
}
