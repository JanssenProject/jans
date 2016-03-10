/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.cert.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.OCSPRespGenerator;
import org.bouncycastle.ocsp.RevokedStatus;
import org.bouncycastle.ocsp.SingleResp;
import org.python.bouncycastle.cert.ocsp.CertificateStatus;
import org.xdi.oxauth.cert.validation.model.ValidationStatus;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.CertificateValidity;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.ValidatorSourceType;
import org.xdi.oxauth.model.util.SecurityProviderUtility;

/**
 * Certificate verifier based on OCSP
 * 
 * @author Yuriy Movchan
 * @version March 10, 2016
 */
public class OCSPCertificateVerifier {

	private static final Logger log = Logger.getLogger(OCSPCertificateVerifier.class);

	public OCSPCertificateVerifier() {
		SecurityProviderUtility.installBCProvider();
	}

	public ValidationStatus validate(X509Certificate certificate, X509Certificate issuer, Date validationDate) {
		ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidatorSourceType.OCSP, CertificateValidity.UNKNOWN);

		try {
			Principal subjectDN = certificate.getSubjectDN();

			String ocspUrl = getOCSPUrl(certificate);
			if (ocspUrl == null) {
				log.error("OCSP URL for '" + subjectDN + "' is empty");
				return status;
			}

			log.debug("OCSP URL for '" + subjectDN + "' is '" + ocspUrl + "'");

			// Generate OCSP request
			OCSPReq ocspReq = generateOCSPRequest(certificate, issuer);

			// Get OCSP response from server
			OCSPResp ocspResp = requestOCSPResponse(ocspUrl, ocspReq);
			if (ocspResp.getStatus() != OCSPRespGenerator.SUCCESSFUL) {
				log.error("OCSP response is invalid");
				return status;
			}

			CertificateID certificateId = new CertificateID(CertificateID.HASH_SHA1, issuer, certificate.getSerialNumber());

			BasicOCSPResp basicOCSPResp = (BasicOCSPResp) ocspResp.getResponseObject();
			SingleResp[] singleResps = basicOCSPResp.getResponses();
			for (SingleResp singleResp : singleResps) {
				CertificateID responseCertificateId = singleResp.getCertID();
				if (!certificateId.equals(responseCertificateId)) {
					continue;
				}

				log.debug("OCSP validationDate: " + validationDate);
				log.debug("OCSP thisUpdate: " + singleResp.getThisUpdate());
				log.debug("OCSP nextUpdate: " + singleResp.getNextUpdate());

				status.setRevocationObjectIssuingTime(basicOCSPResp.getProducedAt());

				Object certStatus = singleResp.getCertStatus();
				if (certStatus == CertificateStatus.GOOD) {
					log.debug("OCSP status is valid for '" + certificate.getSubjectX500Principal() + "'");
					status.setValidity(CertificateValidity.VALID);
				} else {
					if (singleResp.getCertStatus() instanceof RevokedStatus) {
						log.warn("OCSP status is revoked for: " + subjectDN);
						if (validationDate.before(((RevokedStatus) singleResp.getCertStatus()).getRevocationTime())) {
							log.warn("OCSP revocation time after the validation date, the certificate '" + subjectDN + "' was valid at " + validationDate);
							status.setValidity(CertificateValidity.VALID);
						} else {
							Date revocationDate = ((RevokedStatus) singleResp.getCertStatus()).getRevocationTime();
							log.info("OCSP for certificate '" + subjectDN + "' is revoked since " + revocationDate);
							status.setRevocationDate(revocationDate);
							status.setRevocationObjectIssuingTime(singleResp.getThisUpdate());
							status.setValidity(CertificateValidity.REVOKED);
						}
					}
				}
			}

			log.error("There is no matching OCSP response entries");
		} catch (Exception ex) {
			log.error("OCSP exception: ", ex);
		}

		return status;
	}

	private OCSPReq generateOCSPRequest(X509Certificate certificate, X509Certificate issuer) throws OCSPException {
		OCSPReqGenerator ocspReqGenerator = new OCSPReqGenerator();

		CertificateID certId = new CertificateID(CertificateID.HASH_SHA1, issuer, certificate.getSerialNumber());
		ocspReqGenerator.addRequest(certId);

		OCSPReq ocspReq = ocspReqGenerator.generate();
		return ocspReq;
	}

	@SuppressWarnings({ "deprecation", "resource" })
	private String getOCSPUrl(X509Certificate certificate) throws IOException {
		byte[] authInfoAccessExtensionValue = certificate.getExtensionValue(X509Extensions.AuthorityInfoAccess.getId());
		if (authInfoAccessExtensionValue == null) {
			return null;
		}

		DEROctetString oct = (DEROctetString) (new ASN1InputStream(new ByteArrayInputStream(authInfoAccessExtensionValue)).readObject());
		AuthorityInformationAccess authorityInformationAccess = new AuthorityInformationAccess((ASN1Sequence) new ASN1InputStream(oct.getOctets()).readObject());

		AccessDescription[] accessDescriptions = authorityInformationAccess.getAccessDescriptions();
		for (AccessDescription accessDescription : accessDescriptions) {
			boolean correctAccessMethod = accessDescription.getAccessMethod().equals(X509ObjectIdentifiers.ocspAccessMethod);
			if (!correctAccessMethod) {
				continue;
			}

			GeneralName name = accessDescription.getAccessLocation();
			if (name.getTagNo() != GeneralName.uniformResourceIdentifier) {
				continue;
			}

			String str;
			if (name.getDERObject() instanceof DERTaggedObject) {
				DERIA5String derStr = (DERIA5String) ((DERTaggedObject) name.getDERObject()).getObject();
				str = derStr.toString();
			} else {
				DERIA5String derStr = DERIA5String.getInstance(name.getDERObject());
				str = derStr.getString();
			}

			return str;
		}

		return null;

	}

	public OCSPResp requestOCSPResponse(String url, OCSPReq ocspReq) throws IOException, MalformedURLException {
		byte[] ocspReqData = ocspReq.getEncoded();

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		try {
			con.setRequestProperty("Content-Type", "application/ocsp-request");
			con.setRequestProperty("Accept", "application/ocsp-response");

			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);

			OutputStream out = con.getOutputStream();
			try {
				IOUtils.write(ocspReqData, out);
				out.flush();
			} finally {
				IOUtils.closeQuietly(out);
			}

			byte[] responseBytes = IOUtils.toByteArray(con.getInputStream());
			OCSPResp ocspResp = new OCSPResp(responseBytes);

			return ocspResp;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

}
