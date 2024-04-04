package io.jans.casa.plugins.bioid;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.conf.OIDCClientSettings;
import io.jans.casa.model.ApplicationConfiguration;

/**
 * 
 * @author madhumita
 *
 */

public class BioIdService {

	private static BioIdService SINGLE_INSTANCE = null;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private int TIMEOUT = 5000; // 5 seconds
	private static ObjectMapper mapper;
	private IPersistenceService persistenceService;
	private OIDCClientSettings cls;

	private BioIdService() {
		persistenceService = Utils.managedBean(IPersistenceService.class);
		mapper = new ObjectMapper();
		cls = persistenceService.get(ApplicationConfiguration.class, "ou=casa,ou=configuration,o=jans")
				.getSettings().getOidcSettings().getClient();

	}

	public static BioIdService getInstance() {
		if (SINGLE_INSTANCE == null) {
			synchronized (BioIdService.class) {
				SINGLE_INSTANCE = new BioIdService();
			}
		}
		return SINGLE_INSTANCE;
	}

	public OIDCClientSettings getCasaClient() {
		return cls;
	}

	public String generateBioIdCode(byte seedLength) {
		byte[] seed = new byte[seedLength];
		new SecureRandom().nextBytes(seed);
		String dictionary = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		StringBuilder result = new StringBuilder();
		for (byte value : seed) {
			result.append(dictionary.charAt(Math.abs(value) % dictionary.length()));
		}
		return result.toString();
	}

	public Map<String, Map<String, Object>> getJansCredential(String userId) {
		try {
			BioIdPersonModel user = persistenceService.get(BioIdPersonModel.class,
					persistenceService.getPersonDn(userId));
			return user.getJansCredential();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;
	}

	public void setJansCredential(String userId, Map<String, Map<String, Object>> jansCredential) {
		try {
			BioIdPersonModel user = persistenceService.get(BioIdPersonModel.class,
					persistenceService.getPersonDn(userId));
			user.setJansCredential(jansCredential);
			persistenceService.modify(user);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}
