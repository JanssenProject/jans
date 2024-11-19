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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with user authenticators
 *
 * @author Yuriy Movchan
 * @version 03/29/2024
 */
@ApplicationScoped
public class UserAuthenticatorService {

	@Inject
	private Logger log;

	public static String EXTERNAL_UID_FORMAT = "%s:%s";
	private static String[] EMPTY_STRING_ARRAY = new String[0];
	
	private static final UserAuthenticatorList EMPTY_USER_AUTHENTICATOR_LIST = new UserAuthenticatorList(Collections.emptyList());

	public UserAuthenticatorList getUserAuthenticatorList(SimpleUser user) {
		if ((user == null) || (user.getAuthenticator() == null) || (user.getAuthenticator().getAuthenticators() == null)) {
			return EMPTY_USER_AUTHENTICATOR_LIST;
		}

		return user.getAuthenticator();
	}

	public List<UserAuthenticator> getUserAuthenticatorsByType(SimpleUser user, String type) {
		UserAuthenticatorList userAuthenticatorList = getUserAuthenticatorList(user);
		
		List<UserAuthenticator> result = new ArrayList<>();
		for (UserAuthenticator authenticator :  userAuthenticatorList.getAuthenticators()) {
			if (authenticator.getType().equals(type)) {
				result.add(authenticator);
			}
		}

		return result;
	}

	public UserAuthenticator getUserAuthenticatorById(SimpleUser user, String id) {
		UserAuthenticatorList userAuthenticatorList = getUserAuthenticatorList(user);
		
		for (UserAuthenticator authenticator :  userAuthenticatorList.getAuthenticators()) {
			if (authenticator.getId().equals(id)) {
				return authenticator;
			}
		}

		return null;
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
			if (userAuthenticatorList.getAuthenticators().size() == 0) {
				user.setAuthenticator(null);
			}
		}

		List<String> externalUidList = new ArrayList<>();
		if (user.getExternalUid() != null) {
			externalUidList.addAll(Arrays.asList(user.getExternalUid()));
			
			for (Iterator<String> it = externalUidList.iterator(); it.hasNext();) {
				String externalUid = (String) it.next();
				int idx = externalUid.indexOf(':');
				if (idx != -1) {
					String foundType = externalUid.substring(0, idx).trim();
					String id = externalUid.substring(idx + 1).trim();
					if (userAuthenticator.getId().equals(id) && userAuthenticator.getType().equals(foundType)) {
						it.remove();
						break;
					}
				}
			}
			
			if (externalUidList.size() == 0) {
				user.setExternalUid(null);
			} else {
				user.setExternalUid(externalUidList.toArray(EMPTY_STRING_ARRAY));
			}
		}
	}

	public void removeUserAuthenticator(SimpleUser user, String type) {
		UserAuthenticatorList userAuthenticatorList = user.getAuthenticator();
		if (userAuthenticatorList != null) {
			for (Iterator<UserAuthenticator> it = userAuthenticatorList.getAuthenticators().iterator(); it.hasNext();) {
				UserAuthenticator authenticator = (UserAuthenticator) it.next();
				if (type.equals(authenticator.getType())) {
					it.remove();
				}
			}
			if (userAuthenticatorList.getAuthenticators().size() == 0) {
				user.setAuthenticator(null);
			}
		}

		List<String> externalUidList = new ArrayList<>();
		if (user.getExternalUid() != null) {
			externalUidList.addAll(Arrays.asList(user.getExternalUid()));
			
			for (Iterator<String> it = externalUidList.iterator(); it.hasNext();) {
				String externalUid = (String) it.next();
				int idx = externalUid.indexOf(':');
				if (idx != -1) {
					String foundType = externalUid.substring(0, idx).trim();
					if (type.equals(foundType)) {
						it.remove();
					}
				}
			}
			
			if (externalUidList.size() == 0) {
				user.setExternalUid(null);
			} else {
				user.setExternalUid(externalUidList.toArray(EMPTY_STRING_ARRAY));
			}
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
