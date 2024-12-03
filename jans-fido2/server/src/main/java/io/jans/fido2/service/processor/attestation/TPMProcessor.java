/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.processor.attestation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseKeyType;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.SignatureVerifier;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tss.tpm.TPMS_ATTEST;
import tss.tpm.TPMS_CERTIFY_INFO;
import tss.tpm.TPMT_PUBLIC;
import tss.tpm.TPM_GENERATED;


/**
 * Attestation processor for attestations of fmt = tpm
 *
 */
/**
 * 
 */
@ApplicationScoped
public class TPMProcessor implements AttestationFormatProcessor {
	 private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;
    
	@Inject
	private Logger log;

	@Inject
	private CertificateService certificateService;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private AttestationCertificateService attestationCertificateService;

	@Inject
	private SignatureVerifier signatureVerifier;

	@Inject
	private CertificateVerifier certificateVerifier;

	@Inject
	private DataMapperService dataMapperService;

	@Inject
	private Base64Service base64Service;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@Inject
	private CoseService coseService;
	
	@Override
	public AttestationFormat getAttestationFormat() {
		return AttestationFormat.tpm;
	}

	@Override
	public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
			CredAndCounterData credIdAndCounters) {
		log.debug("attStmt : "+attStmt.toString());
		String pubArea = attStmt.get("pubArea").asText();
		String certInfo = attStmt.get("certInfo").asText();
		
		JsonNode cborPublicKey;
		try {
			cborPublicKey = dataMapperService.cborReadTree(authData.getCosePublicKey());
			log.debug("cborPublicKey"+ cborPublicKey);
		} catch (IOException e) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation: " + e.getMessage());
		}

		verifyVersion2(attStmt);
		int alg = verifyAlg(attStmt);

		byte[] hashedBuffer = getHashedBuffer(alg, authData.getAttestationBuffer(), clientDataHash);

		// if attestation mode is enabled in the global config
		if (appConfiguration.getFido2Configuration().getAttestationMode()
				.equalsIgnoreCase(AttestationMode.DISABLED.getValue()) == false) {
			Iterator<JsonNode> i = attStmt.get("x5c").elements();

			
			if (i.hasNext()) {
				ArrayList<String> aikCertificatePath = new ArrayList<String>();
				aikCertificatePath.add(i.next().asText());
				ArrayList<String> certificatePath = new ArrayList<String>();

				while (i.hasNext()) {
					certificatePath.add(i.next().asText());
				}

				List<X509Certificate> certificates = certificateService.getCertificates(certificatePath);
				List<X509Certificate> aikCertificates = certificateService.getCertificates(aikCertificatePath);
				List<X509Certificate> trustAnchorCertificates = attestationCertificateService
						.getAttestationRootCertificates(authData, aikCertificates);
				X509Certificate aikCertificate = aikCertificates.get(0);

				X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(certificates,
						trustAnchorCertificates);
				verifyAIKCertificate(aikCertificate, verifiedCert);

				verifyTPMCertificateExtenstion(aikCertificate, authData);

				String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));
				byte[] certInfoBuffer = base64Service.decode(certInfo);
				byte[] signatureBytes = base64Service.decode(signature.getBytes());

				signatureVerifier.verifySignature(signatureBytes, certInfoBuffer, aikCertificate, alg);

				byte[] pubAreaBuffer = base64Service.decode(pubArea);
				TPMT_PUBLIC tpmtPublic = commonVerifiers.tpmParseToPublic(pubAreaBuffer);
				TPMS_ATTEST tpmsAttest = commonVerifiers.tpmParseToAttest(certInfoBuffer);


				
				verifyMagicInTpms(tpmsAttest);
				verifyTPMSCertificateName(tpmtPublic, tpmsAttest, pubAreaBuffer);
				verifyTPMSExtraData(hashedBuffer, tpmsAttest.extraData);
				verifyThatKeysAreSame1(cborPublicKey,tpmtPublic);
				
				//verifyThatKeysAreSame(tpmtPublic, keyBufferFromAuthData, authData.);
				
			} else {
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
						"Problem with TPM attestation. Unsupported");
			}
		}
		else
		{
			log.debug("In Global fido configuration, AttestationMode is DISABLED, hence skipping the attestation check");
		}
		credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
		credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
		credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
		credIdAndCounters.setAuthenticatorName(attestationCertificateService.getAttestationAuthenticatorName(authData));
		credIdAndCounters.setSignatureAlgorithm(alg);
	}

	/**
	 * Verify that the public key specified by the parameters and unique fields of
	 * pubArea is identical to the credentialPublicKey in the attestedCredentialData
	 * in authenticatorData.
	 * 
	 * @param uncompressedECPointNode
	 * @param tpmtPublic
	 * @return
	 */
	public PublicKey verifyThatKeysAreSame1(JsonNode uncompressedECPointNode, TPMT_PUBLIC tpmtPublic) {

		// Check that pubArea.unique is set to the same public key, as the one in
		// “authData” struct.
		int keyToUse = uncompressedECPointNode.get("1").asInt();
		int algorithmToUse = uncompressedECPointNode.get("3").asInt();

		// algorithm used for attestation
		CoseKeyType keyType = CoseKeyType.fromNumericValue(keyToUse);

		log.debug("keyToUse" + keyToUse);
		log.debug("algorithmToUse : " + algorithmToUse);
		log.debug("keyType" + keyType);

		byte[] tpm = tpmtPublic.unique.toTpm();
		log.debug("tpmtPublic.nameAlg : " + tpmtPublic.nameAlg.toString());
		log.debug("tpmtPublic.parameters.toString() : " + tpmtPublic.parameters.toString());

		log.debug("tpmtPublic.unique.toTpm()- keyBufferFromTPM: " + tpm);

		switch (keyType) {
		case RSA: {

			// TODO: replace with constants
			
			if (algorithmToUse == -65535 || algorithmToUse == -257 ) 
			 {

				byte[] keyBufferFromTPM = Arrays.copyOfRange(tpm, 2, tpm.length);

				byte[] rsaKey_n = base64Service.decode(uncompressedECPointNode.get("-1").asText());
				byte[] rsaKey_e = base64Service.decode(uncompressedECPointNode.get("-2").asText());
				if (!Arrays.equals(keyBufferFromTPM, rsaKey_n)) {
					log.error("verifyThatKeysAreSame RSA" + keyBufferFromTPM + ":" + rsaKey_n);
					throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
							"Problem with TPM attestation.");
				}
			 }
			 else {
				throw new Fido2RuntimeException("Don't know what to do with this key" + keyType + ":" + algorithmToUse);
			}
			
		}
		case EC2: {

			if (algorithmToUse == -7) 
			// TODO: CoseEC2Algorithm.ES256.getNumericValue()
			 {
				int curve = uncompressedECPointNode.get("-1").asInt();
				byte[] x = base64Service.decode(uncompressedECPointNode.get("-2").asText());
				byte[] y = base64Service.decode(uncompressedECPointNode.get("-3").asText());
				byte[] buffer = ByteBuffer.allocate(1 + x.length + y.length).put(UNCOMPRESSED_POINT_INDICATOR).put(x)
						.put(y).array();
				log.debug("pubArea.unique : " + tpm + ":" + buffer);
				if (!Arrays.equals(tpm, buffer)) {
					log.error("verifyThatKeysAreSame EC" + tpm + ":" + buffer);
					throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
							"Problem with TPM attestation.");
				}
			}
			 else {
				throw new Fido2RuntimeException(
						"Don't know what to do with this key" + keyType + " and algorithm " + algorithmToUse);
			}
			
		}
		case OKP: {
			throw new Fido2RuntimeException("Don't know what to do with this key" + keyType);
		}
		default:
			throw new Fido2RuntimeException("Don't know what to do with this key" + keyType);
		}
	}
	 
	
	


	/**
	 *  //  Verify that the public key specified by the parameters and unique fields of pubArea
    // is identical to the credentialPublicKey in the attestedCredentialData in authenticatorData.
	 * 
	 * @param tpmtPublic
	 * @param keyBufferFromAuthData
	 */
	private void verifyThatKeysAreSame(TPMT_PUBLIC tpmtPublic, byte[] keyBufferFromAuthData) {
		byte[] tmp = tpmtPublic.unique.toTpm();
		byte[] keyBufferFromTPM = Arrays.copyOfRange(tmp, 2, tmp.length);

		if (!Arrays.equals(keyBufferFromTPM, keyBufferFromAuthData)) {
			log.error("verifyThatKeysAreSame"+tpmtPublic.unique.toTpm()+":"+keyBufferFromTPM);
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation.");
		}
	}

	private void verifyTPMSExtraData(byte[] hashedBuffer, byte[] extraData) {
		if (!Arrays.equals(hashedBuffer, extraData)) {
			log.error("verifyTPMSExtraData"+hashedBuffer+":"+extraData);
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation.");
		}
	}

	private void verifyTPMSCertificateName(TPMT_PUBLIC tpmtPublic, TPMS_ATTEST tpmsAttest, byte[] pubAreaBuffer) {

		byte[] pubAreaDigest;
		switch (tpmtPublic.nameAlg.asEnum()) {
		case SHA1:
		case SHA256: {
			pubAreaDigest = DigestUtils.getSha256Digest().digest(pubAreaBuffer);
		}
			break;
		default:
			log.error("verifyTPMSCertificateName"+":"+tpmtPublic.nameAlg.asEnum());
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation");
		}
		// this is not really certificate info but nameAlgID + hex.encode(pubAreaDigest)
		// reverse engineered from FIDO Certification tool

		TPMS_CERTIFY_INFO certifyInfo = (TPMS_CERTIFY_INFO) tpmsAttest.attested;
		byte[] certificateName = Arrays.copyOfRange(certifyInfo.name, 2, certifyInfo.name.length);
		if (!Arrays.equals(certificateName, pubAreaDigest)) {
			log.error(certificateName+":"+pubAreaDigest);
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation.");
		}
	}

	private void verifyMagicInTpms(TPMS_ATTEST tpmsAttest) {
		if (tpmsAttest.magic.toInt() != TPM_GENERATED.VALUE.toInt()) {
			log.error(tpmsAttest.magic.toInt() +":"+ TPM_GENERATED.VALUE.toInt());
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation");
		}
	}

	private byte[] getHashedBuffer(int digestAlgorith, byte[] authenticatorDataBuffer, byte[] clientDataHashBuffer) {
		MessageDigest hashedBufferDigester = signatureVerifier.getDigest(digestAlgorith);
		hashedBufferDigester.update(authenticatorDataBuffer);
		hashedBufferDigester.update(clientDataHashBuffer);
		return hashedBufferDigester.digest();
	}

	private void verifyTPMCertificateExtenstion(X509Certificate aikCertificate, AuthData authData) {
		byte[] ext = aikCertificate.getExtensionValue("1 3 6 1 4 1 45724 1 1 4");
		if (ext != null && ext.length > 0) {
			String fidoAAGUID = new String(ext, Charset.forName("UTF-8"));
			if (!authData.getAaguid().equals(fidoAAGUID)) {
				log.error("authData.getAaguid() :"+ authData.getAaguid() + " and fidoAAGUID : "+ fidoAAGUID);
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
						"Problem with TPM attestation : verifyTPMCertificateExtenstion");
			}
		}
	}

	private void verifyAIKCertificate(X509Certificate aikCertificate, X509Certificate rootCertificate) {
		try {
			aikCertificate.verify(rootCertificate.getPublicKey());
		} catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException
				| SignatureException e) {
			log.error("Problem with AIK certificate {}", e.getMessage());
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"Problem with TPM attestation");
		}
	}

	private void verifyVersion2(JsonNode attStmt) {
		if (!attStmt.has("ver")) {
			log.error("TPM does not contain the ver");
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"TPM does not contain the 'ver'");
		}
		String version = attStmt.get("ver").asText();
		if (!version.equals("2.0")) {
			log.error("TPM invalid version, ver 2.0 is required");
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"TPM invalid version, ver 2.0 is required");
		}
	}

	private int verifyAlg(JsonNode attStmt) {
		if (!attStmt.has("alg")) {
			log.error("TPM does not contain the alg");
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.TPM_ERROR,
					"TPM does not contain the 'alg'");
		}
		int alg = attStmt.get("alg").asInt();
		log.debug("TPM attStmt 'alg': {}", alg);
		return alg;
	}
}
