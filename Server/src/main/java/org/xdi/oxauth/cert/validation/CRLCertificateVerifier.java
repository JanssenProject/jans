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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Certificate verifier based on CRL
 * 
 * @author Yuriy Movchan
 * @version March 10, 2016
 */
public class CRLCertificateVerifier implements CertificateVerifier {

	private static final Logger log = Logger.getLogger(CRLCertificateVerifier.class);

	private int maxCrlSize;

	private LoadingCache<String, X509CRL> crlCache;

	public CRLCertificateVerifier(final int maxCrlSize) {
		SecurityProviderUtility.installBCProvider(true);

		this.maxCrlSize = maxCrlSize;
		
		CacheLoader<String, X509CRL> checkedLoader = new CacheLoader<String, X509CRL>() {
			public X509CRL load(String crlURL) throws CertificateException, CRLException, NoSuchProviderException, NoSuchParserException, StreamParsingException, MalformedURLException, IOException, ExecutionException {
				X509CRL result = requestCRL(crlURL);
				Preconditions.checkNotNull(result);

				return result;
			}
		};

		this.crlCache = CacheBuilder.newBuilder().maximumSize(10).expireAfterWrite(60, TimeUnit.MINUTES).build(checkedLoader);
	}

	@Override
	public ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate) {
		X509Certificate issuer = issuers.get(0);
		ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidatorSourceType.CRL, CertificateValidity.UNKNOWN);

		try {
			Principal subjectX500Principal = certificate.getSubjectX500Principal();

			String crlURL = getCrlUri(certificate);
			if (crlURL == null) {
				log.error("CRL's URL for '" + subjectX500Principal + "' is empty");
				return status;
			}

			log.debug("CRL's URL for '" + subjectX500Principal + "' is '" + crlURL + "'");

			X509CRL x509crl = getCrl(crlURL);
			if (!validateCRL(x509crl, certificate, issuer, validationDate)) {
				log.error("The CRL is not valid!");
				status.setValidity(CertificateValidity.INVALID);
				return status;
			}

			X509CRLEntry crlEntry = x509crl.getRevokedCertificate(certificate.getSerialNumber());
			if (crlEntry == null) {
				log.debug("CRL status is valid for '" + subjectX500Principal + "'");
				status.setValidity(CertificateValidity.VALID);
			} else if (crlEntry.getRevocationDate().after(validationDate)) {
				log.warn("CRL revocation time after the validation date, the certificate '" + subjectX500Principal + "' was valid at " + validationDate);
				status.setRevocationObjectIssuingTime(x509crl.getThisUpdate());
				status.setValidity(CertificateValidity.VALID);
			} else {
				log.info("CRL for certificate '" + subjectX500Principal + "' is revoked since " + crlEntry.getRevocationDate());
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
		Principal subjectX500Principal = certificate.getSubjectX500Principal();

		if (x509crl == null) {
			log.error("No CRL found for certificate '" + subjectX500Principal + "'");
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
			log.error("The CRL must be signed by the issuer '" + subjectX500Principal + "' but instead is signed by '"
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
			log.error("There is no KeyUsage extension for certificate '" + subjectX500Principal + "'");
			return false;
		}

		if (!issuerCertificate.getKeyUsage()[6]) {
			log.error("cRLSign bit is not set for CRL certificate'" + subjectX500Principal + "'");
			return false;
		}

		return true;

	}

	private X509CRL getCrl(String url) throws CertificateException, CRLException, NoSuchProviderException, NoSuchParserException, StreamParsingException,
			MalformedURLException, IOException, ExecutionException {
		if (!(url.startsWith("http://") || url.startsWith("https://"))) {
			log.error("It's possbiel to downloid CRL via HTTP and HTTPS only");
			return null;
		}
		
		String cacheKey = url.toLowerCase();
		X509CRL crl = crlCache.get(cacheKey);

		return crl;
	}

	public X509CRL requestCRL(String url) throws IOException, MalformedURLException, CertificateException, CRLException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		try {
			con.setUseCaches(false);

			InputStream in = new BoundedInputStream(con.getInputStream(), maxCrlSize);
			try {
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
				X509CRL crl = (X509CRL) certificateFactory.generateCRL(in);
				log.debug("CRL size: " + crl.getEncoded().length + " bytes");

				return crl;
			} finally {
				IOUtils.closeQuietly(in);
			}
		} catch (IOException ex) {
			log.error("Failed to download CRL from '" + url + "'", ex);
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

	@Override
	public void destroy() {
		crlCache.cleanUp();
	}

}
