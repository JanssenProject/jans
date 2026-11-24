/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.orm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.testng.SkipException;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

/**
 * Creates a SQL entry manager from a profile directory of {@code jans-orm/integration-test} (the directory
 * that holds {@code conf/jans-sql.properties}, {@code conf/salt} and, for TLS, the certificate the
 * properties refer to). The decrypted properties stay in memory and are never logged.
 */
public final class SqlTestProfile {

    public static final String PROFILE_PROPERTY = "jans.test.orm.profile";

    private static final String SQL_PREFIX = "sql#";
    private static final String SSL_FACTORY_ARG = "connection.driver-property.sslfactoryarg";
    private static final String FILE_URL_PREFIX = "file:";

    private SqlTestProfile() {
    }

    /**
     * @throws SkipException when the system property {@value #PROFILE_PROPERTY} is not set
     */
    public static PersistenceEntryManager createEntryManager() throws IOException {
        String profile = System.getProperty(PROFILE_PROPERTY);
        if (StringUtils.isBlank(profile)) {
            throw new SkipException("DB profile is not set; run with -D" + PROFILE_PROPERTY + "=<profile directory>");
        }

        File confDir = new File(profile, "conf");
        Properties sqlProperties = load(new File(confDir, "jans-sql.properties"));
        String encodeSalt = load(new File(confDir, "salt")).getProperty("encodeSalt");
        if (StringUtils.isBlank(encodeSalt)) {
            throw new IllegalStateException("Profile salt file has no encodeSalt");
        }

        Properties decrypted;
        try {
            decrypted = PropertiesDecrypter.decryptAllProperties(StringEncrypter.instance(encodeSalt.trim()), sqlProperties);
        } catch (StringEncrypter.EncryptionException ex) {
            throw new IllegalStateException("Failed to decrypt the profile properties", ex);
        }
        String sslFactoryArg = decrypted.getProperty(SSL_FACTORY_ARG);
        if ((sslFactoryArg != null) && sslFactoryArg.startsWith(FILE_URL_PREFIX)) {
            File certificate = new File(sslFactoryArg.substring(FILE_URL_PREFIX.length()));
            if (!certificate.isAbsolute()) {
                decrypted.setProperty(SSL_FACTORY_ARG, FILE_URL_PREFIX + new File(confDir, certificate.getPath()).getAbsolutePath());
            }
        }

        Properties connectionProperties = new Properties();
        for (String name : decrypted.stringPropertyNames()) {
            connectionProperties.setProperty(SQL_PREFIX + name, decrypted.getProperty(name));
        }

        SqlEntryManagerFactory factory = new SqlEntryManagerFactory();
        factory.create();
        return factory.createEntryManager(connectionProperties);
    }

    private static Properties load(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

}
