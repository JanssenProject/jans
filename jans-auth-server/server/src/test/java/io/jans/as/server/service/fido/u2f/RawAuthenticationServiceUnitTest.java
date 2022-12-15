package io.jans.as.server.service.fido.u2f;

import static org.testng.Assert.assertTrue;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import io.jans.as.server.crypto.signature.SHA256withECDSASignatureVerification;
import io.jans.as.server.crypto.signature.SignatureVerification;
import io.jans.as.model.exception.SignatureException;
import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.as.model.util.SecurityProviderUtility;
import org.testng.annotations.Test;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class RawAuthenticationServiceUnitTest {

	@Test
	public void checkSignatureVerification() throws DecoderException, SignatureException {
		SecurityProviderUtility.installBCProvider();

		String signedDataHex = "415141414141677752674968414c4f4f62544e55506677772d643669776c6a6132636f714134473561374f4156534e744b4462513034717341694541684a734542745072494a49766436636e595351454842415549723644395839794e70636c6166544c797749";
		byte[] signedData = Hex.decodeHex(signedDataHex);

		String signatureDataHex = "3046022100b38e6d33543dfc30f9dea2c258dad9ca2a0381b96bb38055236d2836d0d38aac022100849b0406d3eb20922f77a7276124041c101422be83f57f7236972569f4cbcb02";
		byte[] signatureData = Hex.decodeHex(signatureDataHex);

		String publicKeyHex = "04e9a52ef1136d1eee973c700bd86e1dd314dc04373d47f1219d1f8c286c9f30311fdbb158eaceac60e3a7a0298c94269878c5ec6853004182e126cdb72254edc2";
		byte[] publicKey = Hex.decodeHex(publicKeyHex);

		SignatureVerification signatureVerification = new SHA256withECDSASignatureVerification();

		boolean isValid = signatureVerification.checkSignature(signatureVerification.decodePublicKey(publicKey),
				signedData, signatureData);
		assertTrue(isValid);
	}

	@Test
	public void checkClientDataSignatureVerification() throws DecoderException, SignatureException {
		SecurityProviderUtility.installBCProvider();

		String clientDataHex = "65794a30655841694f694a7559585a705a32463062334975615751755a32563051584e7a5a584a306157397549697769593268686247786c626d646c496a6f694f5659354c56685652475a724e6c64305a453147624459314e3235504e6e4e4756465656635538785157567661465254574842315254647559794973496d39796157647062694936496d68306448427a4f6c7776584339686247786f5957356b637a517a4c6d64736458557562334a6e584339705a47567564476c30655677765958563061474e765a47557561485274496e30";
		byte[] clientData = Hex.decodeHex(clientDataHex);

		String authResponseDataHex = "415141414141677752674968414c4f4f62544e55506677772d643669776c6a6132636f714134473561374f4156534e744b4462513034717341694541684a734542745072494a49766436636e595351454842415549723644395839794e70636c6166544c797749";
		byte[] authResponseData = Hex.decodeHex(authResponseDataHex);

		String publicKeyHex = "04e9a52ef1136d1eee973c700bd86e1dd314dc04373d47f1219d1f8c286c9f30311fdbb158eaceac60e3a7a0298c94269878c5ec6853004182e126cdb72254edc2";
		byte[] publicKey = Hex.decodeHex(publicKeyHex);

		ClientData clientDataObj = new ClientData(new String(clientData));
		RawAuthenticateResponse rawAuthenticateResponse = new RawAuthenticationService()
				.parseRawAuthenticateResponse(new String(authResponseData));

		SignatureVerification signatureVerification = new SHA256withECDSASignatureVerification();

		String appId = "https://allhands43.gluu.org/identity/authcode.htm";
		byte[] signedBytes = packBytesToSign(signatureVerification.hash(appId),
				rawAuthenticateResponse.getUserPresence(), rawAuthenticateResponse.getCounter(),
				signatureVerification.hash(clientDataObj.getRawClientData()));

		boolean isValid = signatureVerification.checkSignature(signatureVerification.decodePublicKey(publicKey),
				signedBytes, rawAuthenticateResponse.getSignature());
		assertTrue(isValid);
	}

	private byte[] packBytesToSign(byte[] appIdHash, byte userPresence, long counter, byte[] challengeHash) {
		ByteArrayDataOutput encoded = ByteStreams.newDataOutput();
		encoded.write(appIdHash);
		encoded.write(userPresence);
		encoded.writeInt((int) counter);
		encoded.write(challengeHash);

		return encoded.toByteArray();
	}

	
	public static void main(String[] args) throws DecoderException, SignatureException {
		RawAuthenticationServiceUnitTest test = new RawAuthenticationServiceUnitTest();
		test.checkClientDataSignatureVerification();
	}
}
