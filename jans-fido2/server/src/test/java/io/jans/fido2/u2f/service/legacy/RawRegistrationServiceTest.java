/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.u2f.service.legacy;

import io.jans.as.model.util.Base64Util;
import io.jans.fido2.model.u2f.message.RawRegisterResponse;
import io.jans.fido2.legacy.service.RawRegistrationService;
import io.jans.fido2.u2f.BaseTest;
import org.python.bouncycastle.util.encoders.Hex;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class RawRegistrationServiceTest extends BaseTest {

    @Test
    public void testSecureClickRawRegistrationResponse() {
        String secureClickResponseHex = "83028b0504437390db40c114e3876bda46b3d5094821b396f8d56a08898b9af79ef98d119edc3dea4ee3459570dfa1886ef85114ad4ac1ffcd0cc3ddc576321273738c9dbb50b9c513cc01f03d6334eb01c6e6e951832556015a1057ace235d41b965fc9feba729678c707d7dc0b5fcecad7dd18b338d834649a750a6fb2ed89292f8183193fcd24341931431a09e00b745cb8523b84308201ac30820153a0030201020204782a0eb9300a06082a8648ce3d0403023046311c301a060355040a1313564153434f2044617461205365637572697479312630240603550403131d564153434f20444947495041535320536563757265436c69636b204341301e170d3136303232323038333930305a170d3431303232323038333930305a3053311c301a060355040a1313564153434f2044617461205365637572697479313330310603550403132a564153434f20444947495041535320536563757265436c69636b204174746573746174696f6e204b65793059301306072a8648ce3d020106082a8648ce3d030107034200044612a220e578b34f6a891e23d65a9e896498011ea9be3029bccf1a8fca465b176697af67e0d912386d4844df233c01e014bad9de9b3932614e65d94c21bfcc83a322302030090603551d13040230003013060b2b0601040182e51c020101040403020560300a06082a8648ce3d04030203470030440220395e8b68c043a77c8fdc4c6ef9b1194d393b694ce5bf616ae944b0cb1c7bcc60022011ccd27a799710e4fe5b0a64c0cff32feff505f79dc43d4753087937c317b105304402202831ab846ac0d61001e3a884077a8e8dc04c99d87f7cb6a5c8880113e5b82e0302201fb3a62fd44847fbc7e422a0d125eb34d67419098a46a6ed3285db986c6c01d89000";
        byte[] secureClickResponseBytes = Hex.decode(secureClickResponseHex);

        // Skip first 3 and last 2 bytes
        // 0x83h - U2F message type
        // 0x028bh - Length of whole message
        byte[] u2fClickResponseBytes = new byte[secureClickResponseBytes.length - 3 - 2];
        System.arraycopy(secureClickResponseBytes, 3, u2fClickResponseBytes, 0, u2fClickResponseBytes.length);

        // Base64 URL encode to allow consume by API
        String u2fResponseBase64 = Base64Util.base64urlencode(u2fClickResponseBytes);
        RawRegistrationService rawRegistrationService = new RawRegistrationService();

        RawRegisterResponse rawRegisterResponse = rawRegistrationService.parseRawRegisterResponse(u2fResponseBase64);

        assertNotNull(rawRegisterResponse.getUserPublicKey());
        assertEquals(rawRegisterResponse.getKeyHandle().length, 80);

        // Check attestation certificate
        assertNotNull(rawRegisterResponse.getAttestationCertificate());
        assertEquals(rawRegisterResponse.getAttestationCertificate().getSigAlgName(), "SHA256WITHECDSA");
        assertEquals(rawRegisterResponse.getAttestationCertificate().getSubjectDN().getName(), "O=VASCO Data Security,CN=VASCO DIGIPASS SecureClick Attestation Key");

        assertEquals(rawRegisterResponse.getSignature().length, 70);
    }

}
