/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.Test;

import java.security.Security;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class HashUtilTest {

    private static final String INPUT = "a308bb8f-25b0-4b1f-85a6-778698a35a43";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void s256Hash() {
        assertEquals("hhNHO19gwnEguTE5SAK-GA", HashUtil.getHash(INPUT, SignatureAlgorithm.ES256));
        assertEquals("hhNHO19gwnEguTE5SAK-GA", HashUtil.getHash(INPUT, SignatureAlgorithm.HS256));
        assertEquals("hhNHO19gwnEguTE5SAK-GA", HashUtil.getHash(INPUT, SignatureAlgorithm.PS256));
        assertEquals("hhNHO19gwnEguTE5SAK-GA", HashUtil.getHash(INPUT, SignatureAlgorithm.RS256));
    }

    @Test
    public void s384Hash() {
        assertEquals("W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ", HashUtil.getHash(INPUT, SignatureAlgorithm.ES384));
        assertEquals("W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ", HashUtil.getHash(INPUT, SignatureAlgorithm.HS384));
        assertEquals("W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ", HashUtil.getHash(INPUT, SignatureAlgorithm.PS384));
        assertEquals("W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ", HashUtil.getHash(INPUT, SignatureAlgorithm.RS384));
    }

    @Test
    public void s512Hash() {
        assertEquals("CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4", HashUtil.getHash(INPUT, SignatureAlgorithm.ES512));
        assertEquals("CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4", HashUtil.getHash(INPUT, SignatureAlgorithm.HS512));
        assertEquals("CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4", HashUtil.getHash(INPUT, SignatureAlgorithm.PS512));
        assertEquals("CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4", HashUtil.getHash(INPUT, SignatureAlgorithm.RS512));
    }
}
