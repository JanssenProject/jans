package io.jans.fido2.service.mds;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.mds.AuthenticatorCertificationStatus;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.service.cdi.event.ApplicationInitialized;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class MdsService {

	@Inject
	private Logger log;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private TocService tocService;

	@Inject
	private DataMapperService dataMapperService;

	@Inject
	private Base64Service base64Service;

	@Inject
	private AppConfiguration appConfiguration;

	private Map<String, JsonNode> mdsEntries;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
		this.mdsEntries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
	}

	public JsonNode fetchMetadata(byte[] aaguidBuffer) {

		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		if (fido2Configuration == null) {
			throw new Fido2RuntimeException("Fido2 configuration not exists");
		}

		String aaguid = deconvert(aaguidBuffer);

		JsonNode mdsEntry = mdsEntries.get(aaguid);
		if (mdsEntry != null) {
			return mdsEntry;
		}

		JsonNode tocEntry = tocService.getAuthenticatorsMetadata(aaguid);

		if (tocEntry == null) {
			throw new Fido2RuntimeException("Authenticator not in TOC aaguid " + aaguid);
		}

		verifyTocEntryStatus(aaguid, tocEntry);

		return tocEntry;
	}

	private void verifyTocEntryStatus(String aaguid, JsonNode tocEntry) {
		JsonNode statusReports = tocEntry.get("statusReports");
		log.debug("========================> Verifying statusReports for AAGUID {}: {}", aaguid, statusReports);

		Iterator<JsonNode> iter = statusReports.elements();
		while (iter.hasNext()) {
			JsonNode statusReport = iter.next();
			AuthenticatorCertificationStatus authenticatorStatus = AuthenticatorCertificationStatus
					.valueOf(statusReport.get("status").asText());
			String authenticatorEffectiveDate = statusReport.get("effectiveDate").asText();
			verifyStatusAcceptable(aaguid, authenticatorStatus);
		}
	}

	private String deconvert(byte[] aaguidBuffer) {
		String converted = Hex.encodeHexString(aaguidBuffer).replaceFirst(
				"([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
		return converted;
	}

	private void verifyStatusAcceptable(String aaguid, AuthenticatorCertificationStatus status) {
		final List<AuthenticatorCertificationStatus> undesiredAuthenticatorStatus = Arrays.asList(
				AuthenticatorCertificationStatus.USER_VERIFICATION_BYPASS,
				AuthenticatorCertificationStatus.ATTESTATION_KEY_COMPROMISE,
				AuthenticatorCertificationStatus.USER_KEY_REMOTE_COMPROMISE,
				AuthenticatorCertificationStatus.USER_KEY_PHYSICAL_COMPROMISE,
				AuthenticatorCertificationStatus.NOT_FIDO_CERTIFIED,
				AuthenticatorCertificationStatus.REVOKED
		);
		if (undesiredAuthenticatorStatus.contains(status)) {
			throw new Fido2RuntimeException("Authenticator " + aaguid + " status undesirable " + status);
		}
	}

	public void clear() {
		this.mdsEntries.clear();
	}
} 
