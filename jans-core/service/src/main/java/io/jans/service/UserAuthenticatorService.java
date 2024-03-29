package io.jans.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import io.jans.model.user.SimpleUser;
import io.jans.model.user.authenticator.UserAuthenticator;
import io.jans.model.user.authenticator.UserAuthenticatorList;
import jakarta.inject.Inject;

/**
 * Provides operations with user authenticators
 *
 * @author Yuriy Movchan
 * @version 03/29/2024
 */
public class UserAuthenticatorService {

	@Inject
	private Logger log;

	public static String EXTERNAL_UID_FORMAT = "%s: %s";
	private static String[] EMPTY_STRING_ARRAY = new String[0];

	public UserAuthenticatorList getUserAuthenticatorList(SimpleUser user) {
		if (user == null) {
			return (UserAuthenticatorList) Collections.EMPTY_LIST;
		}

		return user.getAuthenticator();
	}

	public void addUserAuthenticator(SimpleUser user, UserAuthenticator userAuthenticator) {
		UserAuthenticatorList userAuthenticatorList = user.getAuthenticator();
		if (userAuthenticatorList == null) {
			user.setAuthenticator(new UserAuthenticatorList());
		}

		user.getAuthenticator().addAuthenticator(userAuthenticator);

		String externalUid = String.format(EXTERNAL_UID_FORMAT, userAuthenticator.getType(), userAuthenticator.getId());

		List<String> externalUidList = new ArrayList<>();
		if (user.getExternalUid() != null) {
			externalUidList.addAll(Arrays.asList(user.getExternalUid()));
		}
		externalUidList.add(externalUid);

		user.setExternalUid(externalUidList.toArray(EMPTY_STRING_ARRAY));
	}

	public void removeUserAuthenticator(SimpleUser user, UserAuthenticator userAuthenticator) {
		UserAuthenticatorList userAuthenticatorList = user.getAuthenticator();
		if (userAuthenticatorList != null) {
			for (Iterator<UserAuthenticator> it = userAuthenticatorList.getAuthenticators().iterator(); it.hasNext();) {
				UserAuthenticator authenticator = (UserAuthenticator) it.next();
				if (userAuthenticator.getId().equals(authenticator.getId()) && userAuthenticator.getType().equals(authenticator.getType())) {
					it.remove();
					break;
				}
			}
		}

		List<String> externalUidList = new ArrayList<>();
		if (user.getExternalUid() != null) {
			externalUidList.addAll(Arrays.asList(user.getExternalUid()));
			
			for (Iterator<String> it = externalUidList.iterator(); it.hasNext();) {
				String externalUid = (String) it.next();
				int idx = externalUid.indexOf(':');
				if (idx != -1) {
					String type = externalUid.substring(0, idx).trim();
					String id = externalUid.substring(idx + 1).trim();
					if (userAuthenticator.getId().equals(id) && userAuthenticator.getType().equals(type)) {
						it.remove();
						break;
					}
				}
			}
			
			user.setExternalUid(externalUidList.toArray(EMPTY_STRING_ARRAY));
		}
	}

	public UserAuthenticator createUserAuthenticator(String id, String type) {
		return createUserAuthenticator(id, type, null);
	}

	public UserAuthenticator createUserAuthenticator(String id, String type, Map<String, Object> custom) {
		UserAuthenticator userAuthenticator = new UserAuthenticator(id, type);
		userAuthenticator.setCustom(custom);

		return userAuthenticator;
	}

	public String formatExternalUid(String id, String type) {
		String externalUid = String.format(EXTERNAL_UID_FORMAT, type, id);

		return externalUid;
	}

	public boolean checkAndMigrateToAuthenticatorList(SimpleUser user) {
		if (user.getExternalUid() == null) {
			return false;
		}

		UserAuthenticatorList userAuthenticatorList = user.getAuthenticator();
		if (((userAuthenticatorList != null) && (userAuthenticatorList.getAuthenticators() != null)
				&& (userAuthenticatorList.getAuthenticators().size() > 0))) {
			return false;
		}

		List<String> externalUidList = new ArrayList<>();
		externalUidList.addAll(Arrays.asList(user.getExternalUid()));

		for (String externalUid : user.getExternalUid()) {
			int idx = externalUid.indexOf(':');
			if (idx != -1) {
				String type = externalUid.substring(0, idx).trim();
				String id = externalUid.substring(idx + 1).trim();
				
				UserAuthenticator authenticator = createUserAuthenticator(id, type);
				addUserAuthenticator(user, authenticator);
			}
		}

		return true;
	}

}
