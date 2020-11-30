/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwe;

import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.exception.InvalidJweException;

/**
 * @author Javier Rojas Blum Date: 12.04.2012
 */
public interface JweDecrypter {

    public KeyEncryptionAlgorithm getKeyEncryptionAlgorithm();

    public void setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm keyEncryptionAlgorithm);

    public BlockEncryptionAlgorithm getBlockEncryptionAlgorithm();

    public void setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm blockEncryptionAlgorithm);

    public Jwe decrypt(String encryptedJwe) throws InvalidJweException;
}