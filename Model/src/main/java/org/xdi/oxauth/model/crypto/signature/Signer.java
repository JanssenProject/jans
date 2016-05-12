/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

/**
 * @author Javier Rojas Blum
 * @version April 22, 2016
 */
public interface Signer {

    public String sign(String signingInput) throws Exception;

    public boolean verifySignature(String signingInput, String signature) throws Exception;
}
