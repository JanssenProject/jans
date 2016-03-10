/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.cert.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.NoSuchParserException;
import org.bouncycastle.x509.util.StreamParsingException;
import org.xdi.oxauth.cert.validation.model.ValidationStatus;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.CertificateValidity;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.ValidatorSourceType;
import org.xdi.oxauth.model.util.SecurityProviderUtility;

/**
 * Certificate verifier based on CRL
 * 
 * @author Yuriy Movchan
 * @version March 10, 2016
 */
public class CRLCertificateVerifier {

	private static final Logger log = Logger.getLogger(CRLCertificateVerifier.class);

	private int maxCrlSize;

	@SuppressWarnings("unused")
	private Cache crlCache;

	public CRLCertificateVerifier(final int maxCrlResponseSize) {
		this(null, maxCrlResponseSize);
	}

	public CRLCertificateVerifier(final Cache crlCache, final int maxCrlSize) {
		SecurityProviderUtility.installBCProvider();

		this.crlCache = crlCache;
		this.maxCrlSize = maxCrlSize;
	}

	public ValidationStatus validate(X509Certificate certificate, X509Certificate issuer, Date validationDate) {
		ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidatorSourceType.CRL, CertificateValidity.UNKNOWN);

		try {
			Principal subjectDN = certificate.getSubjectDN();

			String crlURL = getCrlUri(certificate);
			if (crlURL == null) {
				log.error("CRL's URL for '" + subjectDN + "' is empty");
				return status;
			}

			log.debug("CRL's URL for '" + subjectDN + "' is '" + crlURL + "'");

			X509CRL x509crl = getCrl(crlURL);
			if (!validateCRL(x509crl, certificate, issuer, validationDate)) {
				log.error("The CRL is not valid!");
				return status;
			}

			X509CRLEntry crlEntry = x509crl.getRevokedCertificate(certificate.getSerialNumber());
			if (crlEntry == null) {
				log.debug("CRL status is valid for '" + subjectDN + "'");
				status.setValidity(CertificateValidity.VALID);
			} else if (crlEntry.getRevocationDate().after(validationDate)) {
				log.warn("CRL revocation time after the validation date, the certificate '" + subjectDN + "' was valid at " + validationDate);
				status.setRevocationObjectIssuingTime(x509crl.getThisUpdate());
				status.setValidity(CertificateValidity.VALID);
			} else {
				log.info("CRL for certificate '" + subjectDN + "' is revoked since " + crlEntry.getRevocationDate());
				status.setRevocationObjectIssuingTime(x509crl.getThisUpdate());
				status.setRevocationDate(crlEntry.getRevocationDate());
				status.setValidity(CertificateValidity.REVOKED);
			}

		} catch (Exception ex) {
			log.error("CRL exception: ", ex);
		}

		return status;
	}

	private boolean validateCRL(X509CRL x509crl, X509Certificate certificate, X509Certificate issuerCertificate, Date validationDate) {
		Principal subjectDN = certificate.getSubjectDN();

		if (x509crl == null) {
			log.error("No CRL found for certificate '" + subjectDN + "'");
			return false;
		}

		if (log.isTraceEnabled()) {
			try {
				log.trace("CRL number: " + getCrlNumber(x509crl));
			} catch (IOException ex) {
				log.error("Failed to get CRL number", ex);
			}
		}

		if (!x509crl.getIssuerX500Principal().equals(issuerCertificate.getSubjectX500Principal())) {
			log.error("The CRL must be signed by the issuer '" + issuerCertificate.getSubjectDN() + "' but instead is signed by '"
					+ x509crl.getIssuerX500Principal() + "'");
			return false;
		}

		try {
			x509crl.verify(issuerCertificate.getPublicKey());
		} catch (Exception ex) {
			log.error("The signature verification for CRL cannot be performed", ex);
			return false;
		}

		log.debug("CRL validationDate: " + validationDate);
		log.debug("CRL nextUpdate: " + x509crl.getThisUpdate());
		log.debug("CRL thisUpdate: " + x509crl.getNextUpdate());

		if (x509crl.getNextUpdate() != null && validationDate.after(x509crl.getNextUpdate())) {
			log.error("CRL is too old");
			return false;
		}

		if (issuerCertificate.getKeyUsage() == null) {
			log.error("There is no KeyUsage extension for certificate '" + subjectDN + "'");
			return false;
		}

		if (!issuerCertificate.getKeyUsage()[6]) {
			log.error("cRLSign bit is not set for CRL certificate'" + subjectDN + "'");
			return false;
		}

		return true;

	}

	private X509CRL getCrl(String url) throws CertificateException, CRLException, NoSuchProviderException, NoSuchParserException, StreamParsingException,
			MalformedURLException, IOException {
		if (!(url.startsWith("http://") || url.startsWith("https://"))) {
			log.error("It's possbiel to downloid CRL via HTTP and HTTPS only");
			return null;
		}
		
		String cacheKey = url.toLowerCase();
		if (crlCache != null) {
			Element cacheElement = crlCache.get(cacheKey); 
			if (cacheElement != null) {
				log.debug("Get CRL for url '" + url + "' from cache");
				return (X509CRL) cacheElement.getValue();
			}
		}

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		try {
			con.setUseCaches(false);

			InputStream in = new BoundedInputStream(con.getInputStream(), maxCrlSize);
			try {
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
				X509CRL crl = (X509CRL) certificateFactory.generateCRL(in);
				log.debug("CRL size: " + crl.getEncoded().length + " bytes");

				if (crlCache != null) {
					log.debug("Stroring CRL for url '" + url + "' into cache");
					Element cacheElement = new Element(cacheKey, crl);
					crlCache.put(cacheElement);
				}

				return crl;
			} finally {
				IOUtils.closeQuietly(in);
			}
		} catch (IOException ex) {
			log.error("Faield to download CRL from '" + url + "'", ex);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return null;
	}

	@SuppressWarnings({ "deprecation", "resource" })
	private BigInteger getCrlNumber(X509CRL crl) throws IOException {
		byte[] crlNumberExtensionValue = crl.getExtensionValue(X509Extensions.CRLNumber.getId());
		if (crlNumberExtensionValue == null) {
			return null;
		}

		DEROctetString octetString = (DEROctetString) (new ASN1InputStream(new ByteArrayInputStream(crlNumberExtensionValue)).readObject());
		byte[] octets = octetString.getOctets();
		DERInteger integer = (DERInteger) new ASN1InputStream(octets).readObject();
		BigInteger crlNumber = integer.getPositiveValue();

		return crlNumber;
	}

	@SuppressWarnings({ "deprecation", "resource" })
	public String getCrlUri(X509Certificate certificate) throws IOException {
		byte[] crlDistributionPointsValue = certificate.getExtensionValue(X509Extensions.CRLDistributionPoints.getId());
		if (crlDistributionPointsValue == null) {
			return null;
		}

		DEROctetString oct = (DEROctetString) (new ASN1InputStream(new ByteArrayInputStream(crlDistributionPointsValue)).readObject());
		CRLDistPoint distPoint = new CRLDistPoint(((ASN1Sequence) new ASN1InputStream(oct.getOctets()).readObject()));

		DistributionPoint[] distributionPoints = distPoint.getDistributionPoints();
		for (DistributionPoint distributionPoint : distributionPoints) {
			DistributionPointName distributionPointName = distributionPoint.getDistributionPoint();
			if (DistributionPointName.FULL_NAME != distributionPointName.getType()) {
				continue;
			}

			GeneralNames generalNames = (GeneralNames) distributionPointName.getName();
			GeneralName[] names = generalNames.getNames();
			for (GeneralName name : names) {
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
		}

		return null;
	}

}
