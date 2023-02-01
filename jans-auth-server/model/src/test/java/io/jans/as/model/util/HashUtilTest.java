/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.util.security.SecurityProviderUtility;

import org.testng.annotations.Test;

import java.security.Security;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class HashUtilTest extends BaseTest {

    private static final String INPUT = "a308bb8f-25b0-4b1f-85a6-778698a35a43";

    static {
        Security.addProvider(SecurityProviderUtility.getBCProvider());
    }

    @Test
    public void getHash_s256Hash_rigthHashGenerated() {
        showTitle("getHash_s256Hash_rigthHashGenerated");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.ES256), "hhNHO19gwnEguTE5SAK-GA");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.HS256), "hhNHO19gwnEguTE5SAK-GA");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.ES256K), "hhNHO19gwnEguTE5SAK-GA");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.PS256), "hhNHO19gwnEguTE5SAK-GA");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.RS256), "hhNHO19gwnEguTE5SAK-GA");
    }

    @Test
    public void getHash_s384Hash_rigthHashGenerated() {
        showTitle("getHash_s384Hash_rigthHashGenerated");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.ES384), "W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.HS384), "W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.PS384), "W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.RS384), "W-f-EBbMtR-505d5wk4m78wd6qn1vQkZ");
    }

    @Test
    public void getHash_s512Hash_rigthHashGenerated() {
        showTitle("getHash_s512Hash_rigthHashGenerated");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.ES512), "CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.HS512), "CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.PS512), "CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.RS512), "CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4");
        assertEquals(HashUtil.getHash(INPUT, SignatureAlgorithm.EDDSA), "CCmNwrkP_FbnPPpQ5f96xpXTDuzHSeGd3jGZ_JrPJo4");
    }
}
