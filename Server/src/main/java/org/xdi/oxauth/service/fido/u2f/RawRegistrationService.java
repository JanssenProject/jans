/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.io.IOException;
import java.security.cert.CertificateException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.gluu.util.io.ByteDataInputStream;
import org.slf4j.Logger;
import org.xdi.oxauth.crypto.cert.CertificateParser;
import org.xdi.oxauth.crypto.signature.SHA256withECDSASignatureVerification;
import org.xdi.oxauth.model.exception.SignatureException;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.message.RawRegisterResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.ClientData;
import org.xdi.oxauth.model.util.Base64Util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * Provides operations with U2F RAW registration response
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
@Stateless
@Named
public class RawRegistrationService {

	@Inject
	private Logger log;

	public static final byte REGISTRATION_RESERVED_BYTE_VALUE = (byte) 0x05;
	public static final byte REGISTRATION_SIGNED_RESERVED_BYTE_VALUE = (byte) 0x00;
	public static final long INITIAL_DEVICE_COUNTER_VALUE = -1;

	public static final String REGISTER_FINISH_TYPE = "navigator.id.finishEnrollment";
	public static final String REGISTER_CANCEL_TYPE = "navigator.id.cancelEnrollment";
	public static final String[] SUPPORTED_REGISTER_TYPES = new String[] { REGISTER_FINISH_TYPE, REGISTER_CANCEL_TYPE };

	@Inject
	@Named("sha256withECDSASignatureVerification")
	private SHA256withECDSASignatureVerification signatureVerification;

	public RawRegisterResponse parseRawRegisterResponse(String rawDataBase64) throws BadInputException {
		ByteDataInputStream bis = new ByteDataInputStream(Base64Util.base64urldecode(rawDataBase64));
		try {
			try {
				byte reservedByte = bis.readSigned();
				if (reservedByte != REGISTRATION_RESERVED_BYTE_VALUE) {
					throw new BadInputException("Incorrect value of reserved byte. Expected: " + REGISTRATION_RESERVED_BYTE_VALUE + ". Was: " + reservedByte);
				}
				return new RawRegisterResponse(bis.read(65), bis.read(bis.readUnsigned()), CertificateParser.parseDer(bis), bis.readAll());
			} catch (IOException ex) {
				throw new BadInputException("Failed to parse RAW register response", ex);
			} catch (CertificateException e) {
				throw new BadInputException("Malformed attestation certificate", e);
			}
		} finally {
			IOUtils.closeQuietly(bis);
		}
	}

	public void checkSignature(String appId, ClientData clientData, RawRegisterResponse rawRegisterResponse) throws BadInputException {
		String rawClientData = clientData.getRawClientData();
		byte[] signedBytes = packBytesToSign(signatureVerification.hash(appId), signatureVerification.hash(rawClientData), rawRegisterResponse.getKeyHandle(),
				rawRegisterResponse.getUserPublicKey());
		try {
			signatureVerification.checkSignature(rawRegisterResponse.getAttestationCertificate(), signedBytes, rawRegisterResponse.getSignature());
		} catch (SignatureException ex) {
			throw new BadInputException("Failed to checkSignature", ex);
		}
	}

	private byte[] packBytesToSign(byte[] appIdHash, byte[] clientDataHash, byte[] keyHandle, byte[] userPublicKey) {
		ByteArrayDataOutput encoded = ByteStreams.newDataOutput();
		encoded.write(REGISTRATION_SIGNED_RESERVED_BYTE_VALUE);
		encoded.write(appIdHash);
		encoded.write(clientDataHash);
		encoded.write(keyHandle);
		encoded.write(userPublicKey);

		return encoded.toByteArray();
	}

	public DeviceRegistration createDevice(RawRegisterResponse rawRegisterResponse) throws BadInputException {
		return new DeviceRegistration(Base64Util.base64urlencode(rawRegisterResponse.getKeyHandle()), Base64Util.base64urlencode(rawRegisterResponse
				.getUserPublicKey()), rawRegisterResponse.getAttestationCertificate(), INITIAL_DEVICE_COUNTER_VALUE);
	}

}
