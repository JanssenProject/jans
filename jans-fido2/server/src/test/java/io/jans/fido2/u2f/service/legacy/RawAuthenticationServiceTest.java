/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.u2f.service.legacy;

import io.jans.as.model.util.Base64Util;
import io.jans.fido2.model.u2f.message.RawAuthenticateResponse;
import io.jans.fido2.legacy.service.RawAuthenticationService;
import io.jans.fido2.u2f.BaseTest;
import org.python.bouncycastle.util.encoders.Hex;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class RawAuthenticationServiceTest extends BaseTest {

    @Test
    public void testSecureClickRawAuthenticationResponse() {
        String secureClickResponseHex = "01010000 001a3044 0220652a 4248527f 805a6203 a903e820 20d9d871 3966614b f41b93c9 02c83a9f c56f0220 230283f9 8305f889 d379278b 5fde2e2f d3e68182 08dfff75 3e218b74 a6e56306";
        byte[] secureClickResponseBytes = Hex.decode(secureClickResponseHex);

        // Base64 URL encode to allow consume by API
        String u2fResponseBase64 = Base64Util.base64urlencode(secureClickResponseBytes);
        RawAuthenticationService rawAuthenticationService = new RawAuthenticationService();
        RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(u2fResponseBase64);

        assertNotNull(rawAuthenticateResponse);
    }

}
