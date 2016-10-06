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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.OxElevenCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jwk.KeyType;
import org.xdi.oxauth.model.jwk.Use;
import org.xdi.oxauth.model.util.SecurityProviderUtility;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;


/**
 * Command example:
 * java -cp bcprov-jdk15on-1.54.jar:.jar:bcpkix-jdk15on-1.54.jar:commons-cli-1.2.jar:commons-codec-1.5.jar:commons-lang-2.6.jar:jettison-1.3.jar:log4j-1.2.14.jar:oxauth-model.jar:oxauth.jar org.xdi.oxauth.util.KeyGenerator -h
 * <p/>
 * KeyGenerator -algorithms RS256 RS384 RS512 ES256 ES384 ES512 -keystore /Users/JAVIER/tmp/mykeystore.jks -keypasswd secret -dnname "CN=oxAuth CA Certificates" -expiration 365
 * <p/>
 * KeyGenerator -algorithms RS256 RS384 RS512 ES256 ES384 ES512 -ox11 https://ce.gluu.info:8443/oxeleven/rest/oxeleven/generateKey -expiration 365
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2016
 */
public class KeyGenerator {

    private static final String ALGORITHMS = "algorithms";
    private static final String KEY_STORE_FILE = "keystore";
    private static final String KEY_STORE_PASSWORD = "keypasswd";
    private static final String DN_NAME = "dnname";
    private static final String OXELEVEN_GENERATE_KEY_ENDPOINT = "ox11";
    private static final String EXPIRATION = "expiration";
    private static final String HELP = "h";
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

            Option algorithmsOption = new Option(ALGORITHMS, true, "Signature Algorithms (RS256 RS384 RS512 ES256 ES384 ES512).");
            algorithmsOption.setArgs(Option.UNLIMITED_VALUES);

            options.addOption(algorithmsOption);
            options.addOption(KEY_STORE_FILE, true, "Key Store file.");
            options.addOption(KEY_STORE_PASSWORD, true, "Key Store password.");
            options.addOption(DN_NAME, true, "DN of certificate issuer.");
            options.addOption(OXELEVEN_GENERATE_KEY_ENDPOINT, true, "oxEleven Generate Key Endpoint.");
            options.addOption(EXPIRATION, true, "Expiration in days.");
            options.addOption(HELP, false, "Show help.");
        }

        public void parse() {
            CommandLineParser parser = new BasicParser();

            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption(HELP))
                    help();

                if (cmd.hasOption(ALGORITHMS) && cmd.hasOption(OXELEVEN_GENERATE_KEY_ENDPOINT) && cmd.hasOption(EXPIRATION)) {
                    String[] algorithms = cmd.getOptionValues(ALGORITHMS);
                    String generateKeyEndpoint = cmd.getOptionValue(OXELEVEN_GENERATE_KEY_ENDPOINT);
                    int expiration = Integer.parseInt(cmd.getOptionValue(EXPIRATION));

                    List<SignatureAlgorithm> signatureAlgorithms = SignatureAlgorithm.fromString(algorithms);
                    if (signatureAlgorithms.isEmpty()) {
                        help();
                    } else {
                        try {
                            JSONWebKeySet jwks = new JSONWebKeySet();
                            OxElevenCryptoProvider cryptoProvider = new OxElevenCryptoProvider(generateKeyEndpoint, null, null, null);

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

                                JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
                                key.setX5c(StringUtils.toList(x5c));

                                jwks.getKeys().add(key);
                            }

                            System.out.println(jwks);
                        } catch (Exception e) {
                            log.error("Failed to generate keys", e);
                            help();
                        }
                    }
                } else if (cmd.hasOption(ALGORITHMS) && cmd.hasOption(KEY_STORE_FILE) && cmd.hasOption(KEY_STORE_PASSWORD)
                        && cmd.hasOption(DN_NAME) && cmd.hasOption(EXPIRATION)) {
                    String[] algorithms = cmd.getOptionValues(ALGORITHMS);
                    String keystore = cmd.getOptionValue(KEY_STORE_FILE);
                    String keypasswd = cmd.getOptionValue(KEY_STORE_PASSWORD);
                    String dnName = cmd.getOptionValue(DN_NAME);
                    int expiration = Integer.parseInt(cmd.getOptionValue(EXPIRATION));

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

                                JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
                                key.setX5c(StringUtils.toList(x5c));

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

            formatter.printHelp("KeyGenerator -algorithms alg ... -expiration n_days [-ox11 url] [-keystore path -keypasswd secret -dnname dn_name]", options);
            System.exit(0);
        }
    }
}