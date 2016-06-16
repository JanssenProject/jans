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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;


/**
 * KeyGenerator -algorithms RS256 RS384 RS512 ES256 ES384 ES512 -keystore /Users/JAVIER/tmp/mytestkeystore2 -keypasswd secret -dnname "CN=oxAuth CA Certificates"
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version June 15, 2016
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
            options.addOption("h", "help", false, "show help.");
        }

        public void parse() {
            CommandLineParser parser = new BasicParser();

            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("h"))
                    help();

                if (cmd.hasOption("algorithms") && cmd.hasOption("keystore") && cmd.hasOption("keypasswd") && cmd.hasOption("dnname")) {
                    String[] algorithms = cmd.getOptionValues("algorithms");
                    String keystore = cmd.getOptionValue("keystore");
                    String keypasswd = cmd.getOptionValue("keypasswd");
                    String dnName = cmd.getOptionValue("dnname");

                    List<SignatureAlgorithm> signatureAlgorithms = SignatureAlgorithm.fromString(algorithms);
                    if (signatureAlgorithms.isEmpty()) {
                        help();
                    } else {
                        try {
                            JSONWebKeySet jwks = new JSONWebKeySet();
                            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keystore, keypasswd, dnName);

                            Calendar calendar = new GregorianCalendar();
                            calendar.add(Calendar.MINUTE, 5);

                            for (SignatureAlgorithm signatureAlgorithm : signatureAlgorithms) {
                                JSONObject result = cryptoProvider.generateKey(signatureAlgorithm, calendar.getTimeInMillis());

                                JSONWebKey key = new JSONWebKey();
                                key.setKid(result.getString(KEY_ID));
                                key.setUse(Use.SIGNATURE);
                                key.setAlg(signatureAlgorithm);
                                key.setKty(KeyType.fromString(signatureAlgorithm.getFamily()));
                                key.setCrv(signatureAlgorithm.getCurve());
                                key.setN(result.optString(MODULUS));
                                key.setE(result.optString(EXPONENT));
                                key.setX(result.optString(X));
                                key.setY(result.optString(Y));

                                jwks.getKeys().add(key);
                            }

                            System.out.println(jwks);
                        } catch (Exception e) {
                            help();
                        }
                    }
                } else {
                    help();
                }
            } catch (ParseException e) {
                help();
            }
        }

        private void help() {
            HelpFormatter formatter = new HelpFormatter();

            formatter.printHelp("KeyGenerator -algorithms RS256 RS384 RS512 ES256 ES384 ES512 -keystore /path_to/mykeystore -keypasswd secret -dnname \"CN=oxAuth CA Certificate\"", options);
            System.exit(0);
        }
    }
}