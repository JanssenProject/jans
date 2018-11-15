/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.AESWrapEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.exception.InvalidParameterException;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version August 17, 2016
 */
public class JweEncrypterImpl extends AbstractJweEncrypter {

    private PublicKey publicKey;
    private byte[] sharedSymmetricKey;

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, byte[] sharedSymmetricKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        if (sharedSymmetricKey != null) {
            this.sharedSymmetricKey = sharedSymmetricKey.clone();
        }
    }

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, PublicKey publicKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        this.publicKey = publicKey;
    }

    @Override
    public Jwe encrypt(Jwe jwe) throws InvalidJweException {
        try {
            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey).build();
            RSAEncrypter encrypter = new RSAEncrypter(rsaKey);

            JWEObject jweObject = new JWEObject(
                    JWEHeader.parse(jwe.getHeader().toJsonObject().toString()),
                    new Payload(Base64Util.base64urlencode(jwe.getClaims().toJsonString().getBytes("UTF-8"))));

            jweObject.encrypt(encrypter);
            String encryptedJwe = jweObject.serialize();

            String[] jweParts = encryptedJwe.split("\\.");
            if (jweParts.length != 5) {
                throw new InvalidJwtException("Invalid JWS format.");
            }

            String encodedHeader = jweParts[0];
            String encodedEncryptedKey = jweParts[1];
            String encodedInitializationVector = jweParts[2];
            String encodedCipherText = jweParts[3];
            String encodedIntegrityValue = jweParts[4];

            jwe.setEncodedHeader(encodedHeader);
            jwe.setEncodedEncryptedKey(encodedEncryptedKey);
            jwe.setEncodedInitializationVector(encodedInitializationVector);
            jwe.setEncodedCiphertext(encodedCipherText);
            jwe.setEncodedIntegrityValue(encodedIntegrityValue);
            jwe.setHeader(new JwtHeader(encodedHeader));

            return jwe;
        } catch (Exception e) {
            throw new InvalidJweException(e);
        }
    }

    @Override
    public String generateEncryptedKey(byte[] contentMasterKey) throws InvalidJweException {
        if (getKeyEncryptionAlgorithm() == null) {
            throw new InvalidJweException("The key encryption algorithm is null");
        }
        if (contentMasterKey == null) {
            throw new InvalidJweException("The content master key (CMK) is null");
        }

        try {
            if (getKeyEncryptionAlgorithm() == KeyEncryptionAlgorithm.RSA_OAEP
                    || getKeyEncryptionAlgorithm() == KeyEncryptionAlgorithm.RSA1_5) {
                if (publicKey != null) {
                    Cipher cipher = Cipher.getInstance(getKeyEncryptionAlgorithm().getAlgorithm(), "BC");
                    //Cipher cipher = Cipher.getInstance(getKeyEncryptionAlgorithm().getAlgorithm());

                    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                    byte[] encryptedKey = cipher.doFinal(contentMasterKey);

                    String encodedEncryptedKey = Base64Util.base64urlencode(encryptedKey);
                    return encodedEncryptedKey;
                } else {
                    throw new InvalidJweException("The RSA public key is null");
                }

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

                SecretKeySpec keyEncryptionKey = new SecretKeySpec(sharedSymmetricKey, "AES");
                AESWrapEngine aesWrapEngine = new AESWrapEngine();
                CipherParameters params = new KeyParameter(keyEncryptionKey.getEncoded());
                aesWrapEngine.init(true, params);
                byte[] wrappedKey = aesWrapEngine.wrap(contentMasterKey, 0, contentMasterKey.length);

                String encodedEncryptedKey = Base64Util.base64urlencode(wrappedKey);
                return encodedEncryptedKey;
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
        } catch (InvalidKeyException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchProviderException e) {
            throw new InvalidJweException(e);
        }
    }

    @Override
    public Pair<String, String> generateCipherTextAndIntegrityValue(
            byte[] contentMasterKey, byte[] initializationVector, byte[] additionalAuthenticatedData, byte[] plainText)
            throws InvalidJweException {
        if (getBlockEncryptionAlgorithm() == null) {
            throw new InvalidJweException("The block encryption algorithm is null");
        }
        if (contentMasterKey == null) {
            throw new InvalidJweException("The content master key (CMK) is null");
        }
        if (initializationVector == null) {
            throw new InvalidJweException("The initialization vector is null");
        }
        if (additionalAuthenticatedData == null) {
            throw new InvalidJweException("The additional authentication data is null");
        }
        if (plainText == null) {
            throw new InvalidJweException("The plain text to encrypt is null");
        }

        try {
            if (getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A128GCM
                    || getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A256GCM) {
                /*final SecretKey aesKey = new SecretKeySpec(contentMasterKey, "AES");

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, initializationVector);
                cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

                cipher.updateAAD(additionalAuthenticatedData);

                byte[] cipherOutput = cipher.doFinal(plainText);

                final int tagPos = cipherOutput.length - 16;

                byte[] cipherText = subArray(cipherOutput, 0, tagPos);
                byte[] authenticationTag = subArray(cipherOutput, tagPos, 16);

                String encodedCipherText = Base64Util.base64urlencode(cipherText);
                String encodedAuthenticationTag = Base64Util.base64urlencode(authenticationTag);

                AlgorithmParameters algorithmParameters = cipher.getParameters();
                GCMParameterSpec actualParams = algorithmParameters.getParameterSpec(GCMParameterSpec.class);
                byte[] iv = actualParams.getIV();
                int tLen = actualParams.getTLen();
                initializationVector = iv;

                return new Pair<String, String>(encodedCipherText, encodedAuthenticationTag);*/

                /*SecureRandom secureRandom = new SecureRandom();
                byte[] key = new byte[16];
                secureRandom.nextBytes(key);
                SecretKey secretKey = new SecretKeySpec(key, "AES");

                byte[] iv = new byte[12];
                secureRandom.nextBytes(iv);

                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

                if (additionalAuthenticatedData != null) {
                    cipher.updateAAD(additionalAuthenticatedData);
                }

                byte[] cipherOutput = cipher.doFinal(plainText);

                final int tagPos = cipherOutput.length - 128 / 8;

                byte[] cipherText = subArray(cipherOutput, 0, tagPos);
                byte[] authenticationTag = subArray(cipherOutput, tagPos, 128 / 8);

                String encodedCipherText = Base64Util.base64urlencode(cipherText);
                String encodedAuthenticationTag = Base64Util.base64urlencode(authenticationTag);

                return new Pair<String, String>(encodedCipherText, encodedAuthenticationTag);*/


                SecretKey secretKey = new SecretKeySpec(contentMasterKey, "AES");
                KeyParameter key = new KeyParameter(contentMasterKey);
                final int MAC_SIZE_BITS = 128;
                AEADParameters aeadParameters = new AEADParameters(key, MAC_SIZE_BITS, initializationVector,
                        additionalAuthenticatedData);

                final int macSize = aeadParameters.getMacSize() / 8;
                BlockCipher blockCipher = new AESEngine();
                CipherParameters params = new KeyParameter(secretKey.getEncoded());
                blockCipher.init(true, params);
                GCMBlockCipher aGCMBlockCipher = new GCMBlockCipher(blockCipher);
                aGCMBlockCipher.init(true, aeadParameters);
                int len = aGCMBlockCipher.getOutputSize(plainText.length);
                byte[] out = new byte[len];
                int outOff = aGCMBlockCipher.processBytes(plainText, 0, plainText.length, out, 0);
                outOff += aGCMBlockCipher.doFinal(out, outOff);
                byte[] cipherText = new byte[outOff - macSize];
                System.arraycopy(out, 0, cipherText, 0, cipherText.length);
                byte[] authenticationTag = new byte[macSize];
                System.arraycopy(out, outOff - macSize, authenticationTag, 0, authenticationTag.length);

                String encodedCipherText = Base64Util.base64urlencode(cipherText);
                String encodedAuthenticationTag = Base64Util.base64urlencode(authenticationTag);

                return new Pair<String, String>(encodedCipherText, encodedAuthenticationTag);
            } else if (getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A128CBC_PLUS_HS256
                    || getBlockEncryptionAlgorithm() == BlockEncryptionAlgorithm.A256CBC_PLUS_HS512) {
                byte[] cek = KeyDerivationFunction.generateCek(contentMasterKey, getBlockEncryptionAlgorithm());
                IvParameterSpec parameters = new IvParameterSpec(initializationVector);
                Cipher cipher = Cipher.getInstance(getBlockEncryptionAlgorithm().getAlgorithm(), "BC");
                //Cipher cipher = Cipher.getInstance(getBlockEncryptionAlgorithm().getAlgorithm());
                SecretKeySpec secretKeySpec = new SecretKeySpec(cek, "AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameters);
                byte[] cipherText = cipher.doFinal(plainText);

                String encodedCipherText = Base64Util.base64urlencode(cipherText);

                String securedInputValue = new String(additionalAuthenticatedData, Charset.forName(Util.UTF8_STRING_ENCODING))
                        + "." + encodedCipherText;

                byte[] cik = KeyDerivationFunction.generateCik(contentMasterKey, getBlockEncryptionAlgorithm());
                SecretKey secretKey = new SecretKeySpec(cik, getBlockEncryptionAlgorithm().getIntegrityValueAlgorithm());
                Mac mac = Mac.getInstance(getBlockEncryptionAlgorithm().getIntegrityValueAlgorithm());
                mac.init(secretKey);
                byte[] integrityValue = mac.doFinal(securedInputValue.getBytes(Util.UTF8_STRING_ENCODING));

                String encodedIntegrityValue = Base64Util.base64urlencode(integrityValue);

                return new Pair<String, String>(encodedCipherText, encodedIntegrityValue);
            } else {
                throw new InvalidJweException("The block encryption algorithm is not supported");
            }
        } catch (InvalidCipherTextException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidJweException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchProviderException e) {
            throw new InvalidJweException(e);
        } catch (IllegalBlockSizeException e) {
            throw new InvalidJweException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidJweException(e);
        } catch (BadPaddingException e) {
            throw new InvalidJweException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidJweException(e);
        } catch (NoSuchPaddingException e) {
            throw new InvalidJweException(e);
        } catch (InvalidParameterException e) {
            throw new InvalidJweException(e);
        } /*catch (Exception e) {
            throw new InvalidJweException(e);
        }*/
    }

    /*private byte[] subArray(byte[] byteArray, int beginIndex, int length) {

        byte[] subArray = new byte[length];
        System.arraycopy(byteArray, beginIndex, subArray, 0, subArray.length);
        return subArray;
    }*/
}