/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.util;

import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.ElevenCryptoProvider;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.*;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.as.model.util.StringUtils;
import io.jans.util.StringHelper;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.jans.as.model.jwk.JWKParameter.*;

/**
 * Command example:
 * java -cp bcprov-jdk15on-1.54.jar:.jar:bcpkix-jdk15on-1.54.jar:commons-cli-1.2.jar:commons-codec-1.5.jar:commons-lang-2.6.jar:jettison-1.3.jar:log4j-1.2.14.jar:oxauth-model.jar:oxauth.jar KeyGenerator -h
 * <p/>
 * KeyGenerator -sig_keys RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512 EdDSA -enc_keys RSA1_5 RSA-OAEP RSA-OAEP-256 ECDH-ES ECDH-ES+A128KW ECDH-ES+A192KW ECDH-ES+A256KW -keystore /Users/JAVIER/tmp/mykeystore.jks -keypasswd secret -dnname "CN=Jans Auth CA Certificates" -expiration 365
 * <p/>
 * KeyGenerator -sig_keys RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512 EdDSA -ox11 https://ce.gluu.info:8443/oxeleven/rest/generateKey -expiration 365 -at xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Sergey Manoylo
 * @version September 24, 2021
 */
public class KeyGenerator {

    // properties
    private static final String SIGNING_KEYS = "sig_keys";
    private static final String ENCRYPTION_KEYS = "enc_keys";
    private static final String KEY_STORE_FILE = "keystore";
    private static final String KEY_STORE_PASSWORD = "keypasswd";
    private static final String DN_NAME = "dnname";
    private static final String OXELEVEN_ACCESS_TOKEN = "at";
    private static final String OXELEVEN_GENERATE_KEY_ENDPOINT = "ox11";
    private static final String EXPIRATION = "expiration";
    private static final String EXPIRATION_HOURS = "expiration_hours";
    private static final String KEY_LENGTH = "key_length";
    private static final String HELP = "h";
    private static final String TEST_PROP_FILE = "test_prop_file";

    private static final String KEY_NAME_SUFFIX = "_keyId";

    private static final Logger log;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        log = Logger.getLogger(KeyGenerator.class);
    }

    public static void main(String[] args) {
        new Cli(args).parse();
    }

    public static class Cli {
        private String[] args = null;
        private final Options options = new Options();

        public Cli(String[] args) {
            this.args = args;

            Option signingKeysOption = new Option(SIGNING_KEYS, true,
                    "Signature keys to generate (RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512 EdDSA).");
            signingKeysOption.setArgs(Option.UNLIMITED_VALUES);

            Option encryptionKeysOption = new Option(ENCRYPTION_KEYS, true,
                    "Encryption keys to generate (RSA1_5 RSA-OAEP RSA-OAEP-256 ECDH-ES ECDH-ES+A128KW ECDH-ES+A192KW ECDH-ES+A256KW).");
            encryptionKeysOption.setArgs(Option.UNLIMITED_VALUES);

            options.addOption(signingKeysOption);
            options.addOption(encryptionKeysOption);
            options.addOption(KEY_STORE_FILE, true, "Key Store file.");
            options.addOption(KEY_STORE_PASSWORD, true, "Key Store password.");
            options.addOption(DN_NAME, true, "DN of certificate issuer.");
            options.addOption(OXELEVEN_ACCESS_TOKEN, true, "oxEleven Access Token");
            options.addOption(OXELEVEN_GENERATE_KEY_ENDPOINT, true, "oxEleven Generate Key Endpoint.");
            options.addOption(EXPIRATION, true, "Expiration in days.");
            options.addOption(EXPIRATION_HOURS, true, "Expiration in hours.");
            options.addOption(KEY_LENGTH, true, "Key length");
            options.addOption(TEST_PROP_FILE, true, "Tests property file.");
            options.addOption(HELP, false, "Show help.");
        }

        @SuppressWarnings("java:S1874")
        public void parse() {
            CommandLineParser parser = new BasicParser();

            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption(HELP)) {
                    help();
                }

                if (!((cmd.hasOption(SIGNING_KEYS) || cmd.hasOption(ENCRYPTION_KEYS))
                        && (cmd.hasOption(EXPIRATION) || cmd.hasOption(EXPIRATION_HOURS)))) {
                    help();
                }

                String[] sigAlgorithms = cmd.getOptionValues(SIGNING_KEYS);
                String[] encAlgorithms = cmd.getOptionValues(ENCRYPTION_KEYS);
                List<Algorithm> signatureAlgorithms = cmd.hasOption(SIGNING_KEYS) ? Algorithm.fromString(sigAlgorithms, Use.SIGNATURE) : new ArrayList<>();
                List<Algorithm> encryptionAlgorithms = cmd.hasOption(ENCRYPTION_KEYS) ? Algorithm.fromString(encAlgorithms, Use.ENCRYPTION) : new ArrayList<>();
                if (signatureAlgorithms.isEmpty() && encryptionAlgorithms.isEmpty()) {
                    help();
                }

                KeyGeneratorContext context = new KeyGeneratorContext();
                context.setKeyLength(StringHelper.toInt(cmd.getOptionValue(KEY_LENGTH), 2048));
                context.setExpirationDays(StringHelper.toInt(cmd.getOptionValue(EXPIRATION), 0));
                context.setExpirationHours(StringHelper.toInt(cmd.getOptionValue(EXPIRATION_HOURS), 0));
                context.calculateExpiration();
                context.setTestPropFile(TestPropFile.create(cmd));

                if (cmd.hasOption(OXELEVEN_ACCESS_TOKEN) && cmd.hasOption(OXELEVEN_GENERATE_KEY_ENDPOINT)) {
                    generateKeysWithEleven(cmd, signatureAlgorithms, encryptionAlgorithms, context);
                } else if (cmd.hasOption(KEY_STORE_FILE)
                        && cmd.hasOption(KEY_STORE_PASSWORD)
                        && cmd.hasOption(DN_NAME)) {
                    generateKeysWithJansAuth(cmd, signatureAlgorithms, encryptionAlgorithms, context);
                } else {
                    help();
                }
            } catch (ParseException e) {
                log.error("Failed to generate keys", e);
                help();
            }
        }

        private void generateKeysWithJansAuth(CommandLine cmd, List<Algorithm> signatureAlgorithms, List<Algorithm> encryptionAlgorithms, KeyGeneratorContext context) {
            String keystore = cmd.getOptionValue(KEY_STORE_FILE);
            String keypasswd = cmd.getOptionValue(KEY_STORE_PASSWORD);
            String dnName = cmd.getOptionValue(DN_NAME);

            try {
                SecurityProviderUtility.installBCProvider(true);

                context.setCryptoProvider(new AuthCryptoProvider(keystore, keypasswd, dnName));
                generateKeys(context, signatureAlgorithms, encryptionAlgorithms);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Failed to generate keys with `jans-auth` crypto", e);
                help();
            }
        }

        private void generateKeysWithEleven(CommandLine cmd, List<Algorithm> signatureAlgorithms, List<Algorithm> encryptionAlgorithms, KeyGeneratorContext context) {
            String accessToken = cmd.getOptionValue(OXELEVEN_ACCESS_TOKEN);
            String generateKeyEndpoint = cmd.getOptionValue(OXELEVEN_GENERATE_KEY_ENDPOINT);

            try {
                context.setCryptoProvider(new ElevenCryptoProvider(generateKeyEndpoint,
                        null, null, null, accessToken));

                generateKeys(context, signatureAlgorithms, encryptionAlgorithms);
            } catch (Exception e) {
                log.error("Failed to generate keys with 'eleven' crypto", e);
                help();
            }
        }

        @SuppressWarnings("java:S106")
        private void generateKeys(KeyGeneratorContext context, List<Algorithm> signatureAlgorithms,
                                  List<Algorithm> encryptionAlgorithms) throws CryptoProviderException, IOException {
            JSONWebKeySet jwks = new JSONWebKeySet();

            generateSignatureKeys(context, signatureAlgorithms, jwks, KeyOps.CONNECT);
            generateSignatureKeys(context, signatureAlgorithms, jwks, KeyOps.SSA);
            generateEncryptionKeys(context, encryptionAlgorithms, jwks, KeyOps.CONNECT);
            generateEncryptionKeys(context, encryptionAlgorithms, jwks, KeyOps.SSA);

            context.getTestPropFile().generate();
            System.out.println(jwks);
        }

        private void generateEncryptionKeys(KeyGeneratorContext context, List<Algorithm> encryptionAlgorithms, JSONWebKeySet jwks, KeyOps keyOps) throws CryptoProviderException {
            for (Algorithm algorithm : encryptionAlgorithms) {
                KeyEncryptionAlgorithm encryptionAlgorithm = KeyEncryptionAlgorithm.fromName(algorithm.getParamName());
                JSONObject result = context.getCryptoProvider().generateKey(algorithm, context.calculateExpiration().getTimeInMillis(), context.getKeyLength(), keyOps);

                JSONWebKey key = new JSONWebKey();

                key.setName(algorithm.getOutName());
                key.setDescr(algorithm.getDescription());
                key.setKid(result.getString(KEY_ID));
                key.setUse(Use.ENCRYPTION);
                key.setAlg(algorithm);
                key.setKty(encryptionAlgorithm.getFamily().getKeyType());
                key.setExp(result.optLong(EXPIRATION_TIME));
                key.setCrv(encryptionAlgorithm.getCurve());
                key.setN(result.optString(MODULUS));
                key.setE(result.optString(EXPONENT));
                key.setX(result.optString(X));
                key.setY(result.optString(Y));
                key.setKeyOps(keyOps);

                JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
                key.setX5c(StringUtils.toList(x5c));

                jwks.getKeys().add(key);

                context.getTestPropFile().add(getKeyNameFromAlgorithm(algorithm) + "=" + result.getString(KEY_ID));
            }
        }

        private void generateSignatureKeys(KeyGeneratorContext context, List<Algorithm> signatureAlgorithms, JSONWebKeySet jwks, KeyOps keyOps) throws CryptoProviderException {
            for (Algorithm algorithm : signatureAlgorithms) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
                JSONObject result = context.getCryptoProvider().generateKey(algorithm, context.calculateExpiration().getTimeInMillis(), context.getKeyLength(), keyOps);

                JSONWebKey key = new JSONWebKey();

                key.setName(algorithm.getOutName());
                key.setDescr(algorithm.getDescription());
                key.setKid(result.getString(KEY_ID));
                key.setUse(Use.SIGNATURE);
                key.setAlg(algorithm);
                key.setKty(signatureAlgorithm.getFamily().getKeyType());
                key.setExp(result.optLong(EXPIRATION_TIME));
                key.setCrv(signatureAlgorithm.getCurve());
                key.setN(result.optString(MODULUS));
                key.setE(result.optString(EXPONENT));
                key.setX(result.optString(X));
                key.setY(result.optString(Y));
                key.setKeyOps(keyOps);

                JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
                key.setX5c(StringUtils.toList(x5c));

                jwks.getKeys().add(key);

                context.getTestPropFile().add(getKeyNameFromAlgorithm(algorithm) + "=" + result.getString(KEY_ID));
            }
        }

        private static String getKeyNameFromAlgorithm(Algorithm algorithm) {
            final String keyNamePrefix;
            if (Algorithm.RSA_OAEP.equals(algorithm) || Algorithm.RSA_OAEP_256.equals(algorithm)
                    || Algorithm.ECDH_ES.equals(algorithm) || Algorithm.ECDH_ES_PLUS_A128KW.equals(algorithm)
                    || Algorithm.ECDH_ES_PLUS_A192KW.equals(algorithm)
                    || Algorithm.ECDH_ES_PLUS_A256KW.equals(algorithm)) {
                keyNamePrefix = algorithm.name();
            } else {
                keyNamePrefix = algorithm.getParamName();
            }
            return keyNamePrefix + KEY_NAME_SUFFIX;
        }

        private void help() {
            HelpFormatter formatter = new HelpFormatter();

            formatter.printHelp(
                    "KeyGenerator -sig_keys alg ... -enc_keys alg ... -expiration n_days [-expiration_hours n_hours] [-ox11 url] [-keystore path -keypasswd secret -dnname dn_name]",
                    options);
            System.exit(0);
        }
    }
}