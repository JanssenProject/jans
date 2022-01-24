/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import javax.inject.Inject;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.exception.EncryptionException;
import io.jans.util.security.StringEncrypter;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Javier Rojas Blum Date: 05.30.2012
 * @author Sergey Manoylo Date: 10.14.2021
 */
public class KeyGenerationTest extends BaseTest {

    private static final String DEF_PASSW = "AirmXhWZXJJTAUFOWUhTYrAV"; 
    private static final String DEF_SALT = "GjaMlV6GacebBmb4BYshKMBo";

    private static final String DEF_SRC_MESSAGE = "Some Test Message 12345";
    private static final String DEF_WRONG_ENCR_MESSAGE = "KR22mfxohTl7G312/VdARnofALaVE2bBwXjdoy1971T4ZNqxlO82WImQUgoYjTRq";

    private static final String DEF_WRONG_PASSW = "AirmXhWZXJJTAUFOWUhTYrAY"; 
    private static final String DEF_WRONG_SALT = "GjaM2V6GacebBmb4BYshKMBo";

    private static final String DEF_BIND_PROPERTY_NAME = "ldap#bindPassword";

    private static final String DEF_SOME_PROPERTY_1_NAME = "ldap#someProperty1";
    private static final String DEF_SOME_PROPERTY_2_NAME = "ldap#someProperty2";

    private static final int DEF_NUM_THREADS = 10;
    private static final int DEF_NUM_ITERS = 10000;

    /**
     * Implements Callable for Threads Pool
     * Testing multi-thread usage StringEncrypter
     *
     * @author Sergey Manoylo
     * @version 2021-10-22
     */
    private static class AESGCMThreadCallable implements Callable<Integer> {

        private Integer numOk = 0;
        
        private int id = -1;
        private int numIterations;
        private StringEncrypter stringEncrypter;
        
        public AESGCMThreadCallable(int id, int numIterations, StringEncrypter stringEncrypter) {
            this.id = id;
            this.numIterations = numIterations;
            this.stringEncrypter = stringEncrypter;
        }

        public int getId() {
            return id; 
        }

        public int getNumOk() {
            return numOk; 
        }

        @Override
        public Integer call() throws Exception {
            numOk = 0;
            for(int i = 0; i < numIterations; i++) {
                try {
                    String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
                    String decrMessage = stringEncrypter.decrypt(encrMessage);
                    if(DEF_SRC_MESSAGE.equals(decrMessage)) {
                        numOk++;
                    }
                } catch (EncryptionException e) {
                    System.out.println(e.getMessage());
                }
            }
            return numOk;
        }
    }

    @Inject
    private EncryptionService encryptionService;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Parameters({ "ldapAdminPassword" })
    @Test
    public void encryptLdapPassword(final String ldapAdminPassword) throws Exception {
        showTitle("encryptLdapPassword");

        String password = encryptionService.encrypt(ldapAdminPassword);
        System.out.println("Encrypted LDAP Password: " + password);
    }

    @Parameters({ "ldapAdminPassword" })
    @Test
    public void encryptDecryptLdapPassword(final String ldapAdminPassword) throws Exception {
        showTitle("encryptDecryptLdapPassword");

        System.out.println("LDAP Password: " + ldapAdminPassword);

        String password = encryptionService.encrypt(ldapAdminPassword);
        System.out.println("Encrypted LDAP Password: " + password);

        String decryptedPassword = encryptionService.decrypt(password);
        System.out.println("Decrypted LDAP Password: " + decryptedPassword);

        assertTrue(decryptedPassword.equals(ldapAdminPassword));
    }

    @Test
    public void decryptBindPasswordDefStringEncrypterTest() throws Exception {
        showTitle("decryptBindPasswordDefStringEncrypterTest");

        String passw = configurationFactory.getCryptoConfigurationPassw();
        String salt = configurationFactory.getCryptoConfigurationSalt();
        String alg = configurationFactory.getCryptoConfigurationAlg();

        PersistenceConfiguration persistenceConfiguration = configurationFactory.getPersistenceConfiguration();

        String fileName = persistenceConfiguration.getFileName();

        FileConfiguration fileConfiguration = persistenceConfiguration.getConfiguration();

        String bindPassword = fileConfiguration.getString(DEF_BIND_PROPERTY_NAME);

        System.out.println("fileName = " + fileName);
        System.out.println("Encrypted Password: bindPassword = " + bindPassword);

        StringEncrypter defStringEncrypter = StringEncrypter.defaultInstance();
        defStringEncrypter.init(passw, salt, alg);

        String bindPasswordDecr = defStringEncrypter.decrypt(bindPassword);

        System.out.println("Decrypted Password: bindPasswordDecr = " + bindPasswordDecr);

        assertTrue(true);
    }

    @Test
    public void decryptBindPasswordEncryptionServiceTest() throws Exception {
        showTitle("decryptBindPasswordEncryptionServiceTest");

        PersistenceConfiguration persistenceConfiguration = configurationFactory.getPersistenceConfiguration();

        FileConfiguration fileConfiguration = persistenceConfiguration.getConfiguration();

        String bindPassword = fileConfiguration.getString(DEF_BIND_PROPERTY_NAME);
        String fileName = persistenceConfiguration.getFileName();

        System.out.println("fileName = " + fileName);
        System.out.println("Encrypted Password: bindPassword = " + bindPassword);

        String bindPasswordDecr = encryptionService.decrypt(bindPassword);

        System.out.println("Decrypted Password: bindPasswordDecr = " + bindPasswordDecr);

        assertTrue(true);
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypterTest() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypterTest");

        String passw = configurationFactory.getCryptoConfigurationPassw();
        String salt = configurationFactory.getCryptoConfigurationSalt();
        String alg = configurationFactory.getCryptoConfigurationAlg();

        StringEncrypter stringEncrypter = StringEncrypter.instance(passw, salt, alg);

        System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

        String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
        System.out.println("Encrypted Message: encrMessage = " + encrMessage);

        String decrMessage = stringEncrypter.decrypt(encrMessage);
        System.out.println("Decrypted Message: decrMessage = " + decrMessage);

        assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_AES_CBC_ECB_GCM_Test() throws Exception {
        showTitle("eencryptDecryptSrcMessageStringEncrypter_AES_CBC_ECB_GCM_Test");

        String[] algs = { "AES:AES/CBC/PKCS5Padding:128",
                "AES:AES/CBC/PKCS5Padding:192",
                "AES:AES/CBC/PKCS5Padding:256",
                "AES:AES/ECB/PKCS5Padding:128",
                "AES:AES/ECB/PKCS5Padding:192",
                "AES:AES/ECB/PKCS5Padding:256",
                "AES:AES/GCM/NoPadding:128",
                "AES:AES/GCM/NoPadding:192",
                "AES:AES/GCM/NoPadding:256",
                "AES"
        };

        for(String alg : algs) {
            try {
                StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, DEF_SALT, alg);

                System.out.println("----------------------------------------------");
                System.out.println("Algorithm: alg = " + alg);

                System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

                String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
                System.out.println("Encrypted Message: encrMessage = " + encrMessage);

                String decrMessage = stringEncrypter.decrypt(encrMessage);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
                System.out.println("----------------------------------------------");

            } catch (Exception e) {
                System.out.println("Error (encryptDecryptSrcMessageStringEncrypter_AES_CBC_ECB_GCM_Test) : " + " alg = " + alg + " message = " + e.getMessage());
                assertTrue(false);
            }
        }
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_AES_Test() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypter_AES_Test");

        String alg = StringEncrypter.DEF_AES_ENCRYPTION_SCHEME;

        StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, null, alg);

        System.out.println("----------------------------------------------");
        System.out.println("Algorithm: alg = " + alg);

        System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

        String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
        System.out.println("Encrypted Message: encrMessage = " + encrMessage);

        String decrMessage = stringEncrypter.decrypt(encrMessage);
        System.out.println("Decrypted Message: decrMessage = " + decrMessage);

        assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
        System.out.println("----------------------------------------------");
    } 

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_AES_FailTest() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypter_AES_FailTest");

        String alg = StringEncrypter.DEF_AES_ENCRYPTION_SCHEME;
        try {
            @SuppressWarnings("unused")
            StringEncrypter stringEncrypter = StringEncrypter.instance(null, null, alg);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_AES_CBC_GCM_FailTest() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypter_AES_CBC_GCM_FailTest");

        String[] algs = {
                "AES:AES/CBC/PKCS5Padding:128",
                "AES:AES/CBC/PKCS5Padding:192",
                "AES:AES/CBC/PKCS5Padding:256",
                "AES:AES/GCM/NoPadding:128",
                "AES:AES/GCM/NoPadding:192",
                "AES:AES/GCM/NoPadding:256",
                "AES"
        };

        for(String alg : algs) {
            System.out.println("----------------------------------------------");
            System.out.println("Algorithm: alg = " + alg);

            System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

            StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, DEF_SALT, alg);

            String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
            System.out.println("Encrypted Message: encrMessage = " + encrMessage);

            try {
                String decrMessage = stringEncrypter.decrypt(DEF_WRONG_ENCR_MESSAGE);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(false);

            } catch (Exception e) {
                assertTrue(true);
            }

            try {
                StringEncrypter stringEncrypterWrong = StringEncrypter.instance(DEF_WRONG_PASSW, DEF_SALT, alg);

                String decrMessage = stringEncrypterWrong.decrypt(encrMessage);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(false);

            } catch (Exception e) {
                assertTrue(true);
            }

            try {
                StringEncrypter stringEncrypterWrong = StringEncrypter.instance(DEF_PASSW, DEF_WRONG_SALT, alg);

                String decrMessage = stringEncrypterWrong.decrypt(encrMessage);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(false);

            } catch (Exception e) {
                assertTrue(true);
            }

            try {
                // changing (inverting) bytes of the iv (first and last bytes) 
                byte[] encrMessageArray = encrMessage.getBytes();
                encrMessageArray[0] = (byte)~encrMessageArray[0];
                encrMessageArray[StringEncrypter.DEF_IV_LEN_AES-1] = encrMessageArray[0]; 
                String encrMessageWrong = new String(encrMessageArray);

                String decrMessage = stringEncrypter.decrypt(encrMessageWrong);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertFalse(DEF_SRC_MESSAGE.equals(decrMessage));

            } catch (Exception e) {
                assertTrue(true);
            }

            System.out.println("----------------------------------------------");
        }
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_AES_ECB_FailTest() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypter_AES_ECB_FailTest");

        String[] algs = {
                "AES:AES/ECB/PKCS5Padding:128",
                "AES:AES/ECB/PKCS5Padding:192",
                "AES:AES/ECB/PKCS5Padding:256"
        };

        for(String alg : algs) {
            System.out.println("----------------------------------------------");
            System.out.println("Algorithm: alg = " + alg);

            System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

            StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, DEF_SALT, alg);

            String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
            System.out.println("Encrypted Message: encrMessage = " + encrMessage);

            try {
                String decrMessage = stringEncrypter.decrypt(DEF_WRONG_ENCR_MESSAGE);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(false);

            } catch (Exception e) {
                assertTrue(true);
            }

            try {
                StringEncrypter stringEncrypterWrong = StringEncrypter.instance(DEF_WRONG_PASSW, DEF_SALT, alg);

                String decrMessage = stringEncrypterWrong.decrypt(encrMessage);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(false);

            } catch (Exception e) {
                assertTrue(true);
            }

            try {
                StringEncrypter stringEncrypterWrong = StringEncrypter.instance(DEF_PASSW, DEF_WRONG_SALT, alg);

                String decrMessage = stringEncrypterWrong.decrypt(encrMessage);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(false);

            } catch (Exception e) {
                assertTrue(true);
            }

            try {
                StringEncrypter stringEncrypterWrong = StringEncrypter.instance(DEF_PASSW, DEF_SALT, alg);

                String decrMessage = stringEncrypterWrong.decrypt(encrMessage);
                System.out.println("Decrypted Message: decrMessage = " + decrMessage);

                assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));

            } catch (Exception e) {
                assertTrue(false);
            }

            System.out.println("----------------------------------------------");
        }
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_DES_Test() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypter_DES_Test");

        String alg = StringEncrypter.DEF_DES_ENCRYPTION_SCHEME;

        StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, null, alg);

        System.out.println("----------------------------------------------");
        System.out.println("Algorithm: alg = " + alg);

        System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

        String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
        System.out.println("Encrypted Message: encrMessage = " + encrMessage);

        String decrMessage = stringEncrypter.decrypt(encrMessage);
        System.out.println("Decrypted Message: decrMessage = " + decrMessage);

        assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
        System.out.println("----------------------------------------------");
    }

    @Test
    public void encryptDecryptSrcMessageStringEncrypter_DESede_Test() throws Exception {
        showTitle("encryptDecryptSrcMessageStringEncrypter_DESede_Test");

        String alg = StringEncrypter.DEF_DES_EDE_ENCRYPTION_SCHEME;

        StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, null , alg);

        System.out.println("----------------------------------------------");
        System.out.println("Algorithm: alg = " + alg);

        System.out.println("Source Message: srcMessage = " + DEF_SRC_MESSAGE);

        String encrMessage = stringEncrypter.encrypt(DEF_SRC_MESSAGE);
        System.out.println("Encrypted Message: encrMessage = " + encrMessage);

        String decrMessage = stringEncrypter.decrypt(encrMessage);
        System.out.println("Decrypted Message: decrMessage = " + decrMessage);

        assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
        System.out.println("----------------------------------------------");
    }

    @Test
    public void decryptAllPropertiesTest() throws Exception {
        showTitle("decryptAllPropertiesTest");

        Properties properties = new Properties();

        properties.put(DEF_SOME_PROPERTY_1_NAME, DEF_SRC_MESSAGE);
        properties.put(DEF_SOME_PROPERTY_2_NAME, DEF_SRC_MESSAGE);

        String encryptedMessage = encryptionService.encrypt(DEF_SRC_MESSAGE);

        assertTrue(DEF_SRC_MESSAGE.equals(encryptionService.decrypt(encryptedMessage)));

        properties.put(DEF_BIND_PROPERTY_NAME, encryptedMessage);

        Properties decrProperties = encryptionService.decryptAllProperties(properties);

        assertTrue(DEF_SRC_MESSAGE.equals(decrProperties.getProperty(DEF_BIND_PROPERTY_NAME)));
        assertTrue(DEF_SRC_MESSAGE.equals(decrProperties.getProperty(DEF_SOME_PROPERTY_1_NAME)));
        assertTrue(DEF_SRC_MESSAGE.equals(decrProperties.getProperty(DEF_SOME_PROPERTY_2_NAME)));
    }

    @Test
    public void encryptDecryptAesTest() throws EncryptionException {

        showTitle("encryptDecryptAesTest");

        String[] algs = {
                "AES:AES/CBC/PKCS5Padding:128",
                "AES:AES/CBC/PKCS5Padding:192",
                "AES:AES/CBC/PKCS5Padding:256",
                "AES:AES/GCM/NoPadding:128",
                "AES:AES/GCM/NoPadding:192",
                "AES:AES/GCM/NoPadding:256",
                "AES:AES/ECB/PKCS5Padding:128",
                "AES:AES/ECB/PKCS5Padding:192",
                "AES:AES/ECB/PKCS5Padding:256",
                "AES"
        };

        for(String alg : algs) {
            System.out.println("----------------------------------------------");
            System.out.println("alg: " + alg);
            System.out.println("Source Message: " + DEF_SRC_MESSAGE);

            StringEncrypter encrypter = StringEncrypter.instance( DEF_PASSW, DEF_SALT, alg);
            String encMessage = encrypter.encrypt(DEF_SRC_MESSAGE);
            System.out.println("Encrypted Message: " + encMessage);
            String decrMessage = encrypter.decrypt(encMessage);
            System.out.println("Decrypted Message: " + decrMessage);

            assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
            System.out.println("----------------------------------------------");
        }
        
    }

    @Test
    public void encryptDecryptDesTest() throws EncryptionException {
        
        showTitle("encryptDecryptDesTest");
        
        String[] algs = {
                "DES:DES/CBC/PKCS5Padding:56",
                "DES:DES/ECB/PKCS5Padding:56",
                "DES"
        };

        for(String alg : algs) {
            System.out.println("----------------------------------------------");
            System.out.println("Source Message: " + DEF_SRC_MESSAGE);

            StringEncrypter encrypter = StringEncrypter.instance(DEF_PASSW, null, alg);
            String encMessage = encrypter.encrypt(DEF_SRC_MESSAGE);
            System.out.println("Encrypted Message: " + encMessage);
            String decrMessage = encrypter.decrypt(encMessage);
            System.out.println("Decrypted Message: " + decrMessage);

            assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
            System.out.println("----------------------------------------------");
        }

    }

    @Test
    public void encryptDecryptDesEdeTest() throws EncryptionException {

        showTitle("encryptDecryptDesEdeTest");
        
        String[] algs = {
                "DESede:DESede/CBC/PKCS5Padding:168",
                "DESede:DESede/ECB/PKCS5Padding:168",
                "DESede"
        };

        for(String alg : algs) {
            System.out.println("----------------------------------------------");
            System.out.println("alg: " + alg);
            System.out.println("Source Message: " + DEF_SRC_MESSAGE);

            StringEncrypter encrypter = StringEncrypter.instance(DEF_PASSW, null, alg);
            String encMessage = encrypter.encrypt(DEF_SRC_MESSAGE);
            System.out.println("Encrypted Message: " + encMessage);
            String decrMessage = encrypter.decrypt(encMessage);
            System.out.println("Decrypted Message: " + decrMessage);

            assertTrue(DEF_SRC_MESSAGE.equals(decrMessage));
            System.out.println("----------------------------------------------");
        }

    }

    @Test
    public void encryptDecryptMThreadSrcMessageStringEncrypter_AES_GCM_Test() throws EncryptionException, InterruptedException, ExecutionException {
        showTitle("encryptDecryptMThreadSrcMessageStringEncrypter_AES_GCM_Test");

        System.out.println("----------------------------------------------");
        System.out.println("starting pool threads...");

        String alg = "AES:AES/GCM/NoPadding:256";
        StringEncrypter stringEncrypter = StringEncrypter.instance(DEF_PASSW, DEF_SALT, alg);

        ArrayList<Callable<Integer>> calls = new ArrayList<Callable<Integer>>();

        for(int i = 0; i < DEF_NUM_THREADS; i++) {
            calls.add(new AESGCMThreadCallable(i, DEF_NUM_ITERS, stringEncrypter));
        }

        ExecutorService execService = Executors.newFixedThreadPool(DEF_NUM_THREADS);

        List<Future<Integer>> futures = execService.invokeAll(calls);
        for(int i = 0; i < futures.size(); i++) {
            Future<Integer> future = futures.get(i);
            Callable<Integer> callable = calls.get(i);
            Integer numOk = future.get();
            AESGCMThreadCallable aesGCMCallable = (AESGCMThreadCallable)callable;
            System.out.println(String.format("Thread id = %02d numOk = %05d", aesGCMCallable.getId(), numOk));
            assertTrue(DEF_NUM_ITERS == numOk);
            assertTrue(aesGCMCallable.getNumOk() == numOk);
        }

        System.out.println("pool threads are finished...");
        System.out.println("----------------------------------------------");
    }
