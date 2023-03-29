/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.util;

import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.util.security.SecurityProviderUtility;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.security.PrivateKey;

/**
 * Export private key from JKS Command example: java -cp
 * KeyExporter -h
 * <p/>
 * KeyExporter -keystore /Users/yuriy/tmp/mykeystore.jks -keypasswd secret
 * -alias "2d4817e7-5fe8-4b6b-8f64-fe3723625122"
 * -exportfile=/Users/yuriy/tmp/mykey.pem
 * <p/>
 *
 * @author Yuriy Movchan
 * @version February 12, 2019
 */
public class KeyExporter {

    private static final String KEY_STORE_FILE = "keystore";
    private static final String KEY_STORE_PASSWORD = "keypasswd";
    private static final String KEY_ALIAS = "alias";
    private static final String EXPORT_FILE = "exportfile";
    private static final String HELP = "h";
    private static final Logger log;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        log = Logger.getLogger(KeyExporter.class);
    }

    public static void main(String[] args) throws Exception {
        new Cli(args).parse();
    }

    public static class Cli {
        private String[] args = null;
        private final Options options = new Options();

        public Cli(String[] args) {
            this.args = args;

            options.addOption(KEY_STORE_FILE, true, "Key Store file.");
            options.addOption(KEY_STORE_PASSWORD, true, "Key Store password.");
            options.addOption(KEY_ALIAS, true, "Key alias.");
            options.addOption(EXPORT_FILE, true, "Export file.");
            options.addOption(HELP, false, "Show help.");
        }

        public void parse() {
            CommandLineParser parser = new BasicParser();

            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption(HELP))
                    help();

                if (cmd.hasOption(KEY_STORE_FILE) && cmd.hasOption(KEY_STORE_PASSWORD) && cmd.hasOption(KEY_ALIAS)
                        && cmd.hasOption(EXPORT_FILE)) {

                    String keyStore = cmd.getOptionValue(KEY_STORE_FILE);
                    String keyStorePasswd = cmd.getOptionValue(KEY_STORE_PASSWORD);
                    String keyAlias = cmd.getOptionValue(KEY_ALIAS);
                    String exportFile = cmd.getOptionValue(EXPORT_FILE);

                    try {
                        SecurityProviderUtility.installBCProvider(true);

                        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStore, keyStorePasswd,
                                "CN=Jans Auth CA Certificates");
                        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyAlias);
                        String base64EncodedKey = WordUtils.wrap(new String(Base64.encode(privateKey.getEncoded())), 64,
                                "\n", true);

                        StringBuilder sb = new StringBuilder();
                        SignatureAlgorithm signatureAlgorithm = cryptoProvider.getSignatureAlgorithm(keyAlias);
                        if (AlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily())) {
                            sb.append("-----BEGIN RSA PRIVATE KEY-----\n");
                            sb.append(base64EncodedKey);
                            sb.append("\n");
                            sb.append("-----END RSA PRIVATE KEY-----\n");
                        } else {
                            sb.append("-----BEGIN PRIVATE KEY-----\n");
                            sb.append(base64EncodedKey);
                            sb.append("\n");
                            sb.append("-----END PRIVATE KEY-----\n");
                        }

                        FileUtils.writeStringToFile(new File(exportFile), sb.toString());
                    } catch (Exception e) {
                        log.error("Failed to export key", e);
                        help();
                    }
                } else {
                    help();
                }
            } catch (ParseException e) {
                log.error("Failed to export key", e);
                help();
            }
        }

        private void help() {
            HelpFormatter formatter = new HelpFormatter();

            formatter.printHelp(
                    "KeyExporter -keystore path -keypasswd secret -alias 2d4817e7-5fe8-4b6b-8f64-fe3723625122 -exportfile=export-path",
                    options);
            System.exit(0);
        }
    }
}