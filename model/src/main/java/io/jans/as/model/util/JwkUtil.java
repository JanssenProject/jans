/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.exception.InvalidParameterException;
import io.jans.as.model.jwk.KeyType;

/**
 * JWK utility abstract class.
 * 
 * @author Sergey Manoylo
 * @version September 24, 2021
 */
public class JwkUtil {

	/**
	 * 
	 */
	private JwkUtil() {
	}

	/**
	 * 
	 * @param algFamily
	 * @return
	 * @throws InvalidParameterException
	 */
	public static KeyType getKeyTypeFromAlgFamily(final AlgorithmFamily algFamily) throws InvalidParameterException {
		KeyType keyType = null;
		switch (algFamily) {
		case HMAC:
		case AES:
		case PASSW: {
			keyType = KeyType.OCT;
			break;
		}
		case RSA: {
			keyType = KeyType.RSA;
			break;
		}
		case EC: {
			keyType = KeyType.EC;
			break;
		}
		case ED: {
			keyType = KeyType.OKP;
			break;
		}
		default: {
			throw new InvalidParameterException("Wrong value of AlgorithmFamily: algFamily = " + algFamily);
		}
		}
		return keyType;
	}

}
