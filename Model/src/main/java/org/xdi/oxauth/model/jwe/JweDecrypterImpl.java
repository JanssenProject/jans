/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

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
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidParameterException;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
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
        }
    }
}