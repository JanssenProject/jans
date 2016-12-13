/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import java.io.File;
import java.security.PrivateKey;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.bouncycastle.util.encoders.Base64;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithmFamily;
import org.xdi.oxauth.model.util.SecurityProviderUtility;


/**
 * Export private key from JKS
 * Command example:
 * java -cp org.xdi.oxauth.util.KeyExporter -h
 * <p/>
 * KeyExporter -keystore /Users/yuriy/tmp/mykeystore.jks -keypasswd secret -alias "2d4817e7-5fe8-4b6b-8f64-fe3723625122" -exportfile=/Users/yuriy/tmp/mykey.pem
 * <p/>
 *
 * @author Yuriy Movchan
 * @version 11/23/2016
 */
public class KeyExporter {

    private static final String KEY_STORE_FILE = "keystore";
    private static final String KEY_STORE_PASSWORD = "keypasswd";
    private static final String KEY_ALIAS = "alias";
    private static final String EXPORT_FILE = "exportfile";
    private static final String HELP = "h";
    private static final Logger log;

    static {
        // Add console appender
        LogManager.getRootLogger().removeAllAppenders();

        ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
        LogManager.getRootLogger().addAppender(consoleAppender);

        log = Logger.getLogger(KeyExporter.class);
    }

    public static void main(String[] args) throws Exception {
        new Cli(args).parse();
    }

    public static class Cli {
        private static final Logger log = Logger.getLogger(Cli.class.getName());
        private String[] args = null;
        private Options options = new Options();

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

                if (cmd.hasOption(KEY_STORE_FILE) && cmd.hasOption(KEY_STORE_PASSWORD) &&
                	cmd.hasOption(KEY_ALIAS) && cmd.hasOption(EXPORT_FILE)) {

                	String keyStore = cmd.getOptionValue(KEY_STORE_FILE);
                    String keyStorePasswd = cmd.getOptionValue(KEY_STORE_PASSWORD);
                    String keyAlias = cmd.getOptionValue(KEY_ALIAS);
                    String exportFile = cmd.getOptionValue(EXPORT_FILE);

                    try {
                        SecurityProviderUtility.installBCProvider(true);

                        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStore, keyStorePasswd, "CN=oxAuth CA Certificates");
                        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyAlias);
                    	String base64EncodedKey = WordUtils.wrap(new String(Base64.encode(privateKey.getEncoded())), 64, "\n", true);
                    	
                    	

                    	StringBuilder sb = new StringBuilder();
                        SignatureAlgorithm signatureAlgorithm = cryptoProvider.getSignatureAlgorithm(keyAlias);
                        if (SignatureAlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily())) {
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

            formatter.printHelp("KeyExporter -keystore path -keypasswd secret -alias 2d4817e7-5fe8-4b6b-8f64-fe3723625122 -exportfile=export-path", options);
            System.exit(0);
        }
    }
}