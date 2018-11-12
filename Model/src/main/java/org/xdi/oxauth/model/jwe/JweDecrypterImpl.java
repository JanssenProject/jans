/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.AESWrapEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.exception.InvalidParameterException;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.text.ParseException;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class JweDecrypterImpl extends AbstractJweDecrypter {

    private PrivateKey privateKey;
    private RSAPrivateKey rsaPrivateKey;
    private byte[] sharedSymmetricKey;

    public JweDecrypterImpl(byte[] sharedSymmetricKey) {
        if (sharedSymmetricKey != null) {
            this.sharedSymmetricKey = sharedSymmetricKey.clone();
        }
    }

    public JweDecrypterImpl(RSAPrivateKey rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
    }

    public JweDecrypterImpl(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public Jwe decrypt(String encryptedJwe) throws InvalidJweException {
        try {
            String[] jweParts = encryptedJwe.split("\\.");
            if (jweParts.length != 5) {
                throw new InvalidJwtException("Invalid JWS format.");
            }

            String encodedHeader = jweParts[0];
            String encodedEncryptedKey = jweParts[1];
            String encodedInitializationVector = jweParts[2];
            String encodedCipherText = jweParts[3];
            String encodedIntegrityValue = jweParts[4];

            Jwe jwe = new Jwe();
            jwe.setEncodedHeader(encodedHeader);
            jwe.setEncodedEncryptedKey(encodedEncryptedKey);
            jwe.setEncodedInitializationVector(encodedInitializationVector);
            jwe.setEncodedCiphertext(encodedCipherText);
            jwe.setEncodedIntegrityValue(encodedIntegrityValue);
            jwe.setHeader(new JwtHeader(encodedHeader));

            // jose4j

            JsonWebEncryption receiverJwe = new JsonWebEncryption();

            org.jose4j.jwa.AlgorithmConstraints algConstraints = new org.jose4j.jwa.AlgorithmConstraints(org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST, KeyManagementAlgorithmIdentifiers.RSA_OAEP);
            receiverJwe.setAlgorithmConstraints(algConstraints);
            org.jose4j.jwa.AlgorithmConstraints encConstraints = new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, ContentEncryptionAlgorithmIdentifiers.AES_128_GCM, "A128KW", "A256KW", "A256GCM", "RSA1_5");
            receiverJwe.setContentEncryptionAlgorithmConstraints(encConstraints);

            receiverJwe.setKey(privateKey);

            receiverJwe.setCompactSerialization(encryptedJwe);
            String payload = new String(receiverJwe.getPlaintextBytes());
            jwe.setClaims(new JwtClaims(payload));

            // joseJwt

            /*EncryptedJWT encryptedJwt = EncryptedJWT.parse(encryptedJwe);

            JWEDecrypter decrypter = new RSADecrypter(privateKey);
            decrypter.getJCAContext().setProvider(BouncyCastleProviderSingleton.getInstance());

            encryptedJwt.decrypt(decrypter);

            String payload = encryptedJwt.getPayload().toString();
            jwe.setClaims(new JwtClaims(payload));*/

            return jwe;
        } /*catch (ParseException e) {
            throw new InvalidJweException(e);
        } catch (JOSEException e) {
            throw new InvalidJweException(e);
        } catch (InvalidJwtException e) {
            throw new InvalidJweException(e);
        } catch (JoseException e) {
            throw new InvalidJweException(e);
        }*/ catch (Exception e) {
            throw new InvalidJweException(e);
        }
    }

    @Override
    public byte[] decryptEncryptionKey(String encodedEncryptedKey) throws InvalidJweException {
        if (getKeyEncryptionAlgorithm() == null) {
            throw new InvalidJweException("The key encryption algorithm is null");
        }
        if (encodedEncryptedKey == null) {
            throw new InvalidJweException("The encoded encryption key is null");
        }

        try {
            if (getKeyEncryptionAlgorithm() == KeyEncryptionAlgorithm.RSA_OAEP
                    || getKeyEncryptionAlgorithm() == KeyEncryptionAlgorithm.RSA1_5) {
                if (rsaPrivateKey == null && privateKey == null) {
                    throw new InvalidJweException("The RSA private key is null");
                }

                //Cipher cipher = Cipher.getInstance(getKeyEncryptionAlgorithm().getAlgorithm(), "BC");
                Cipher cipher = Cipher.getInstance(getKeyEncryptionAlgorithm().getAlgorithm());

                if (rsaPrivateKey != null) {
                    KeyFactory keyFactory = KeyFactory.getInstance(getKeyEncryptionAlgorithm().getFamily(), "BC");
                    RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent());
                    java.security.interfaces.RSAPrivateKey privKey = (java.security.interfaces.RSAPrivateKey) keyFactory.generatePrivate(privKeySpec);
                    cipher.init(Cipher.DECRYPT_MODE, privKey);
                } else {
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                }

                byte[] decryptedKey = cipher.doFinal(Base64Util.base64urldecode(encodedEncryptedKey));

                return decryptedKey;
            } else if (getKeyEncryptionAlgorithm() == KeyEncryptionAlgorithm.A128KW
                    || getKeyEncryptionAlgorithm() == KeyEncryptionAlgorithm.A256KW) {
                if (sharedSymmetricKey == null) {
                    throw new InvalidJweException("The shared symmetric key is null");
                }
                if (sharedSymmetricKey.length != 16) { // 128 bit
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    sharedSymmetricKey = sha.digest(sharedSymmetricKey);
                    sharedSymmetricKey = Arrays.copyOf(sharedSymmetricKey, 16);
                }
                byte[] encryptedKey = Base64Util.base64urldecode(encodedEncryptedKey);
                SecretKeySpec keyEncryptionKey = new SecretKeySpec(sharedSymmetricKey, "AES");
                AESWrapEngine aesWrapEngine = new AESWrapEngine();
                CipherParameters params = new KeyParameter(keyEncryptionKey.getEncoded());
                aesWrapEngine.init(false, params);
                byte[] decryptedKey = aesWrapEngine.unwrap(encryptedKey, 0, encryptedKey.length);

                return decryptedKey;
            } else {
                throw new InvalidJweException("The key encryption algorithm is not supported");
            }
        } catch (NoSuchPaddingException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidJweException(e);
        } catch (IllegalBlockSizeException e) {
            throw new InvalidJweException(e);
        } catch (BadPaddingException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchProviderException e) {
            throw new InvalidJweException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidJweException(e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidJweException(e);
        } catch (InvalidCipherTextException e) {
            throw new InvalidJweException(e);
        }
    }

    @Override
    public String decryptCipherText(String encodedCipherText, byte[] contentMasterKey, byte[] initializationVector,
                                    byte[] authenticationTag, byte[] additionalAuthenticatedData) throws InvalidJweException {
        if (getBlockEncryptionAlgorithm() == null) {
            throw new InvalidJweException("The block encryption algorithm is null");
        }
        if (contentMasterKey == null) {
            throw new InvalidJweException("The content master key (CMK) is null");
        }
        if (initializationVector == null) {
            throw new InvalidJweException("The initialization vector is null");
        }
        if (authenticationTag == null) {
            throw new InvalidJweException("The authentication tag is null");
        }
        if (additionalAuthenticatedData == null) {
            throw new InvalidJweException("The additional authentication data is null");
        }

        try {
            if (getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A128GCM
                    || getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A256GCM) {
                /*final SecretKey aesKey = new SecretKeySpec(contentMasterKey, "AES");

                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, initializationVector);
                cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

                cipher.updateAAD(additionalAuthenticatedData);

                byte[] cipherText = Base64Util.base64urldecode(encodedCipherText);
                //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //baos.write(cipherText);
                //baos.write(authenticationTag);

                //byte[] plainTextBytes = cipher.doFinal(baos.toByteArray());

                cipher.update(cipherText);
                byte[] plainTextBytes = cipher.doFinal(authenticationTag);

                String plainText = new String(plainTextBytes, Charset.forName(Util.UTF8_STRING_ENCODING));

                return plainText;*/

                /*byte[] cipherText = Base64Util.base64urldecode(encodedCipherText);
                ByteBuffer byteBuffer = ByteBuffer.wrap(cipherText);
                int ivLength = byteBuffer.getInt();
                if(ivLength < 12 || ivLength >= 16) { // check input parameter
                    throw new IllegalArgumentException("invalid iv length");
                }
                byte[] iv = new byte[ivLength];
                byteBuffer.get(iv);
                byte[] cipherMessage = new byte[byteBuffer.remaining()];
                byteBuffer.get(cipherMessage);

                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(contentMasterKey, "AES"), new GCMParameterSpec(128, iv));
                if (additionalAuthenticatedData != null) {
                    cipher.updateAAD(additionalAuthenticatedData);
                }
                byte[] plainTextBytes = cipher.doFinal(cipherMessage);

                String plainText = new String(plainTextBytes, Charset.forName(Util.UTF8_STRING_ENCODING));

                return plainText;*/


                final int MAC_SIZE_BITS = 128;
                byte[] cipherText = Base64Util.base64urldecode(encodedCipherText);

                KeyParameter key = new KeyParameter(contentMasterKey);
                AEADParameters aeadParameters = new AEADParameters(key, MAC_SIZE_BITS, initializationVector, additionalAuthenticatedData);
                SecretKeySpec sks = new SecretKeySpec(contentMasterKey, "AES");

                BlockCipher blockCipher = new AESEngine();
                CipherParameters params = new KeyParameter(sks.getEncoded());
                blockCipher.init(false, params);
                GCMBlockCipher aGCMBlockCipher = new GCMBlockCipher(blockCipher);
                aGCMBlockCipher.init(false, aeadParameters);
                byte[] input = new byte[cipherText.length + authenticationTag.length];
                System.arraycopy(cipherText, 0, input, 0, cipherText.length);
                System.arraycopy(authenticationTag, 0, input, cipherText.length, authenticationTag.length);
                int len = aGCMBlockCipher.getOutputSize(input.length);
                byte[] out = new byte[len];
                int outOff = aGCMBlockCipher.processBytes(input, 0, input.length, out, 0);
                aGCMBlockCipher.doFinal(out, outOff);

                String plaintext = new String(out, Charset.forName(Util.UTF8_STRING_ENCODING));

                return plaintext;
            } else if (getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A128CBC_PLUS_HS256
                    || getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A256CBC_PLUS_HS512) {
                byte[] cipherText = Base64Util.base64urldecode(encodedCipherText);

                byte[] cek = KeyDerivationFunction.generateCek(contentMasterKey, getBlockEncryptionAlgorithm());
                Cipher cipher = Cipher.getInstance(getBlockEncryptionAlgorithm().getAlgorithm());
                IvParameterSpec ivParameter = new IvParameterSpec(initializationVector);
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), ivParameter);
                byte[] decodedPlainTextBytes = cipher.doFinal(cipherText);
                String decodedPlainText = new String(decodedPlainTextBytes, Charset.forName(Util.UTF8_STRING_ENCODING));

                // Integrity check
                String securedInputValue = new String(additionalAuthenticatedData, Charset.forName(Util.UTF8_STRING_ENCODING))
                        + "." + encodedCipherText;
                byte[] cik = KeyDerivationFunction.generateCik(contentMasterKey, getBlockEncryptionAlgorithm());
                SecretKey secretKey = new SecretKeySpec(cik, getBlockEncryptionAlgorithm().getIntegrityValueAlgorithm());
                Mac mac = Mac.getInstance(getBlockEncryptionAlgorithm().getIntegrityValueAlgorithm());
                mac.init(secretKey);
                byte[] integrityValue = mac.doFinal(securedInputValue.getBytes(Util.UTF8_STRING_ENCODING));
                if (!Arrays.equals(integrityValue, authenticationTag)) {
                    throw new InvalidJweException("The authentication tag is not valid");
                }

                return decodedPlainText;
            } else {
                throw new InvalidJweException("The block encryption algorithm is not supported");
            }
        } catch (InvalidCipherTextException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchPaddingException e) {
            throw new InvalidJweException(e);
        } catch (BadPaddingException e) {
            throw new InvalidJweException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidJweException(e);
        } catch (IllegalBlockSizeException e) {
            throw new InvalidJweException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchProviderException e) {
            throw new InvalidJweException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidJweException(e);
        } catch (InvalidParameterException e) {
            throw new InvalidJweException(e);
        } /*catch (IOException e) {
            throw new InvalidJweException(e);
        }*/
    }
}