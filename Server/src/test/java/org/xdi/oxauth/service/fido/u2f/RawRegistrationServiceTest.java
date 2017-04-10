package org.xdi.oxauth.service.fido.u2f;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang.ArrayUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.python.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.fido.u2f.message.RawAuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.message.RawRegisterResponse;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.util.ArrayHelper;

public class RawRegistrationServiceTest extends BaseComponentTest {

	@Inject
	private RawRegistrationService rawRegistrationService;

	/*
	 * Configuration must be present, otherwise server will not start
	 * normally... There is fallback configuration from file but server will not
	 * work as expected in cluster.`
	 */
	@Test
	public void testCheckSecureClickRawRegistrationResponse() {
		String secureClickResponseHex = "83028b05 04437390 db40c114 e3876bda 46b3d509 004821b3 96f8d56a 08898b9a f79ef98d 119edc3d 01ea4ee3 459570df a1886ef8 5114ad4a c1ffcd0c 02c3ddc5 76321273 738c9dbb 50b9c513 cc01f03d 036334eb 01c6e6e9 51832556 015a1057 ace235d4 041b965f c9feba72 9678c707 d7dc0b5f cecad7dd 0518b338 d834649a 750a6fb2 ed89292f 8183193f 06cd2434 1931431a 09e00b74 5cb8523b 84308201 07ac3082 0153a003 02010202 04782a0e b9300a06 08082a86 48ce3d04 03023046 311c301a 06035504 090a1313 56415343 4f204461 74612053 65637572 0a697479 31263024 06035504 03131d56 4153434f 0b204449 47495041 53532053 65637572 65436c69 0c636b20 4341301e 170d3136 30323232 30383339 0d30305a 170d3431 30323232 30383339 30305a30 0e53311c 301a0603 55040a13 13564153 434f2044 0f617461 20536563 75726974 79313330 31060355 10040313 2a564153 434f2044 49474950 41535320 11536563 75726543 6c69636b 20417474 65737461 1274696f 6e204b65 79305930 1306072a 8648ce3d 13020106 082a8648 ce3d0301 07034200 044612a2 1420e578 b34f6a89 1e23d65a 9e896498 011ea9be 153029bc cf1a8fca 465b1766 97af67e0 d912386d 164844df 233c01e0 14bad9de 9b393261 4e65d94c 1721bfcc 83a32230 20300906 03551d13 04023000 18301306 0b2b0601 040182e5 1c020101 04040302 19056030 0a06082a 8648ce3d 04030203 47003044 1a022039 5e8b68c0 43a77c8f dc4c6ef9 b1194d39 1b3b694c e5bf616a e944b0cb 1c7bcc60 022011cc 1cd27a79 9710e4fe 5b0a64c0 cff32fef f505f79d 1dc43d47 53087937 c317b105 30440220 2831ab84 1e6ac0d6 1001e3a8 84077a8e 8dc04c99 d87f7cb6 1fa5c888 0113e5b8 2e030220 1fb3a62f d44847fb 20c7e422 a0d125eb 34d67419 098a46a6 ed3285db 21986c6c 01d89000";
		byte[] secureClickResponseBytes = Hex.decode(secureClickResponseHex);
		
		// Skip first 3 bytes
		// 0x83h - U2F message type
		// 0x028bh - Length of whole message
		byte[] u2fClickResponseBytes = new byte[secureClickResponseBytes.length - 3];
		System.arraycopy(secureClickResponseBytes, 3, u2fClickResponseBytes, 0, u2fClickResponseBytes.length);
		
		// Base64 URL encode to allow consume by API
		String u2fResponseBase64 = Base64Util.base64urlencode(u2fClickResponseBytes);
		
		RawRegisterResponse rawRegisterResponse = rawRegistrationService.parseRawRegisterResponse(u2fResponseBase64);
		
		assertNotNull(rawRegisterResponse.getUserPublicKey());
		assertEquals(rawRegisterResponse.getKeyHandle().length, 140);

		// Our API can't parser next bytes with attestation certification not exists
		assertNull(rawRegisterResponse.getAttestationCertificate());
		assertEquals(rawRegisterResponse.getSignature().length, 0);
	}

}
