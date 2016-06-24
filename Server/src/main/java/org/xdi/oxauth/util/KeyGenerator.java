/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import org.apache.commons.cli.*;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jwk.KeyType;
import org.xdi.oxauth.model.jwk.Use;
import org.xdi.oxauth.model.util.SecurityProviderUtility;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;


/**
 * Command example:
 * KeyGenerator -algorithms RS256 RS384 RS512 ES256 ES384 ES512
 *              -keystore /Users/JAVIER/tmp/mykeystore.jks
 *              -keypasswd secret
 *              -dnname "CN=oxAuth CA Certificates"
 *              -expiration 365
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version June 16, 2016
 */
public class KeyGenerator {

    private static final Logger log;

    static {
        // Add console appender
        LogManager.getRootLogger().removeAllAppenders();

        ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
        LogManager.getRootLogger().addAppender(consoleAppender);

        log = Logger.getLogger(KeyGenerator.class);
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

            Option algorithmsOption = new Option("algorithms", true, "Algorithms.");
            algorithmsOption.setArgs(Option.UNLIMITED_VALUES);

            options.addOption(algorithmsOption);
            options.addOption("keystore", true, "Key Store file.");
            options.addOption("keypasswd", true, "Key Store password.");
            options.addOption("dnname", true, "DN of certificate issuer.");
            options.addOption("expiration", true, "Expiration in days.");
            options.addOption("h", "help", false, "show help.");
        }

        public void parse() {
            CommandLineParser parser = new BasicParser();

            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("h"))
                    help();

                if (cmd.hasOption("algorithms") && cmd.hasOption("keystore") && cmd.hasOption("keypasswd")
                        && cmd.hasOption("dnname") && cmd.hasOption("expiration")) {
                    String[] algorithms = cmd.getOptionValues("algorithms");
                    String keystore = cmd.getOptionValue("keystore");
                    String keypasswd = cmd.getOptionValue("keypasswd");
                    String dnName = cmd.getOptionValue("dnname");
                    int expiration = Integer.parseInt(cmd.getOptionValue("expiration"));

                    List<SignatureAlgorithm> signatureAlgorithms = SignatureAlgorithm.fromString(algorithms);
                    if (signatureAlgorithms.isEmpty()) {
                        help();
                    } else {
                        try {
                        	SecurityProviderUtility.installBCProvider(true);

                        	JSONWebKeySet jwks = new JSONWebKeySet();
                            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keystore, keypasswd, dnName);

                            Calendar calendar = new GregorianCalendar();
                            calendar.add(Calendar.DATE, expiration);

                            for (SignatureAlgorithm signatureAlgorithm : signatureAlgorithms) {
                                JSONObject result = cryptoProvider.generateKey(signatureAlgorithm, calendar.getTimeInMillis());
                                //System.out.println(result);

                                JSONWebKey key = new JSONWebKey();
                                key.setKid(result.getString(KEY_ID));
                                key.setUse(Use.SIGNATURE);
                                key.setAlg(signatureAlgorithm);
                                key.setKty(KeyType.fromString(signatureAlgorithm.getFamily()));
                                key.setExp(result.optLong(EXPIRATION_TIME));
                                key.setCrv(signatureAlgorithm.getCurve());
                                key.setN(result.optString(MODULUS));
                                key.setE(result.optString(EXPONENT));
                                key.setX(result.optString(X));
                                key.setY(result.optString(Y));

                                jwks.getKeys().add(key);
                            }

                            System.out.println(jwks);
                        } catch (Exception e) {
                        	log.error("Failed to generate keys", e);
                            help();
                        }
                    }
                } else {
                    help();
                }
            } catch (ParseException e) {
            	log.error("Failed to generate keys", e);
                help();
            }
        }

        private void help() {
            HelpFormatter formatter = new HelpFormatter();

            formatter.printHelp("KeyGenerator -algorithms RS256 RS384 RS512 ES256 ES384 ES512 -keystore /path_to/mykeystore.jks -keypasswd secret -dnname \"CN=oxAuth CA Certificate\" -expiration 365", options);
            System.exit(0);
        }
    }
}