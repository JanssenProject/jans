/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;

import io.jans.as.common.model.common.User;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.util.Util;
import io.jans.model.GluuStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.service.DataSourceTypeService;
import io.jans.util.ArrayHelper;
import io.jans.util.StringHelper;

/**
 * Provides operations with users.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version @version August 20, 2019
 */
public abstract class UserService {

	public static final String[] USER_OBJECT_CLASSES = new String[] { AttributeConstants.objectClassPerson };

    @Inject
    private Logger log;

    @Inject
    protected PersistenceEntryManager persistenceEntryManager;

    @Inject
    protected DataSourceTypeService dataSourceTypeService;

    @Inject
    private InumService inumService;

    /**
     * returns User by Dn
     *
     * @return User
     */
    @Nullable
    public User getUserByDn(String dn, String... returnAttributes) {
        if (Util.isNullOrEmpty(dn)) {
            return null;
        }
        return persistenceEntryManager.find(dn, User.class, returnAttributes);
    }

	public User getUserByInum(String inum, String... returnAttributes) {
		if (StringHelper.isEmpty(inum)) {
			return null;
		}
		
		String userDn = getDnForUser(inum);
		User user = getUserByDn(userDn, returnAttributes);
		if (user == null) {
			return null;
		}

		return user;
	}

	public User getUser(String userId, String... returnAttributes) {
		log.debug("Getting user information from LDAP: userId = {}", userId);

		if (StringHelper.isEmpty(userId)) {
			return null;
		}

		String peopleBaseDn = getPeopleBaseDn();
		Filter userUidFilter;
		if (dataSourceTypeService.isSpanner(peopleBaseDn)) {
			userUidFilter = Filter.createEqualityFilter("uid", StringHelper.toLowerCase(userId));
		} else {
			userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), StringHelper.toLowerCase(userId));
		}

		List<User> entries = persistenceEntryManager.findEntries(peopleBaseDn, User.class, userUidFilter, returnAttributes);
		log.debug("Found {} entries for user id = {}", entries.size(), userId);

		if (entries.size() > 0) {
			return entries.get(0);
		} else {
			return null;
		}
	}

	public String getUserInum(User user) {
		if (user == null) {
			return null;
		}
		
		String inum = user.getAttribute("inum");

		return inum;
	}

	public String getUserInum(String userId) {
		User user = getUser(userId, "inum");

		return getUserInum(user);
	}

    public User updateUser(User user) {
        user.setUpdatedAt(new Date());
		persistenceEntryManager.merge(user);

		return getUserByDn(user.getDn());
	}

    public User addDefaultUser(String uid) {
        String peopleBaseDN = getPeopleBaseDn();

        String inum = inumService.generatePeopleInum();

    	User user = new User();
        user.setDn("inum=" + inum + "," + peopleBaseDN);
    	user.setCustomAttributes(Arrays.asList(
    			new CustomObjectAttribute("inum", inum),
    			new CustomObjectAttribute("jansStatus", GluuStatus.ACTIVE.getValue()),
				new CustomObjectAttribute("displayName", "User " + uid + " added via Jans Auth custom plugin")));
    	user.setUserId(uid);

    	List<String> personCustomObjectClassList = getPersonCustomObjectClassList();
    	if ((personCustomObjectClassList != null) && !personCustomObjectClassList.isEmpty()) {
    		user.setCustomObjectClasses(personCustomObjectClassList.toArray(new String[personCustomObjectClassList.size()]));
    	}

    	user.setCreatedAt(new Date());
		persistenceEntryManager.persist(user);
		
		return getUser(uid);
	}

    public User addUser(User user, boolean active) {
        String peopleBaseDN = getPeopleBaseDn();

        String inum = inumService.generatePeopleInum();

        user.setDn("inum=" + inum + "," + peopleBaseDN);
        user.setAttribute("inum", inum, false);

        GluuStatus status = active ? GluuStatus.ACTIVE : GluuStatus.REGISTER;
        user.setAttribute("jansStatus",  status.getValue(), false);

        List<String> personCustomObjectClassList = getPersonCustomObjectClassList();
    	if ((personCustomObjectClassList != null) && !personCustomObjectClassList.isEmpty()) {
    		Set<String> allObjectClasses = new HashSet<>();
    		allObjectClasses.addAll(personCustomObjectClassList);

    		String currentObjectClasses[] = user.getCustomObjectClasses();
    		if (ArrayHelper.isNotEmpty(currentObjectClasses)) {
        		allObjectClasses.addAll(Arrays.asList(currentObjectClasses));
    		}

    		user.setCustomObjectClasses(allObjectClasses.toArray(new String[allObjectClasses.size()]));
    	}

    	user.setCreatedAt(new Date());
    	persistenceEntryManager.persist(user);

		return getUserByDn(user.getDn());
	}

    public User getUserByAttribute(String attributeName, Object attributeValue) {
        return getUserByAttribute(attributeName, attributeValue, null);
    }

    public User getUserByAttribute(String attributeName, Object attributeValue, Boolean multiValued) {
        List<User> entries = getUsersByAttribute(attributeName, attributeValue, multiValued, 1);
        if (entries.size() > 0) {
            return entries.get(0);
        } else {
            return null;
        }
    }

	public User getUniqueUserByAttributes(List<String> attributeNames, String attributeValue) {
		log.debug("Getting user information from LDAP: attributeNames = '{}', attributeValue = '{}'", attributeNames, attributeValue);

		User user = null;

		if (attributeNames != null) {
			for (String attributeName : attributeNames) {
				User searchUser = new User();
				searchUser.setDn(getPeopleBaseDn());

				List<CustomObjectAttribute> customAttributes =  new ArrayList<>();
				customAttributes.add(new CustomObjectAttribute(attributeName, attributeValue));

				searchUser.setCustomAttributes(customAttributes);

				try {
					List<User> entries = persistenceEntryManager.findEntries(searchUser);
					log.debug("Found '{}' entries", entries.size());

					if (entries.size() == 0) {
						continue;
					} else if (entries.size() == 1) {
						user = entries.get(0);
						break;
					} else if (entries.size() > 0) {
						break;
					}
				} catch (Exception e) {
					log.debug(e.getMessage(), e);
				}
			}
		}

		return user;
	}

	public List<User> getUsersByAttribute(String attributeName, Object attributeValue, Boolean multiValued, int limit) {
		log.debug("Getting user information from LDAP: attributeName = '{}', attributeValue = '{}'", attributeName, attributeValue);

		if (StringHelper.isEmpty(attributeName) || (attributeValue == null)) {
			return null;
		}

		Filter filter = Filter.createEqualityFilter(attributeName, attributeValue);
		if (multiValued != null) {
			filter.multiValued(multiValued);
		}

		List<User> entries = persistenceEntryManager.findEntries(getPeopleBaseDn(), User.class, filter, limit);
		log.debug("Found '{}' entries", entries.size());

		return entries;
	}

    public User getUserByAttributes(Object attributeValue, String[] attributeNames, String... returnAttributes) {
		return getUserByAttributes(attributeValue, attributeNames, null, returnAttributes);
	}

	public User getUserByAttributes(Object attributeValue, String[] attributeNames, Boolean multiValued, String... returnAttributes) {
		if (ArrayHelper.isEmpty(attributeNames)) {
			return null;
		}

		log.debug("Getting user information from DB: {} = {}", ArrayHelper.toString(attributeNames), attributeValue);

		String peopleBaseDn = getPeopleBaseDn();

		List<Filter> filters = new ArrayList<Filter>(); 
		for (String attributeName : attributeNames) {
			Filter filter;
			if (dataSourceTypeService.isSpanner(peopleBaseDn)) {
				filter = Filter.createEqualityFilter(attributeName, attributeValue);
			} else {
				filter = Filter.createEqualityFilter(Filter.createLowercaseFilter(attributeName), attributeValue);
			}

			if (multiValued != null) {
	        	filter.multiValued(multiValued);
	        }
			filters.add(filter);
		}

		Filter searchFiler;
		if (filters.size() == 1) {
			searchFiler = filters.get(0);
		} else {
			searchFiler = Filter.createORFilter(filters);
		}

		List<User> entries = persistenceEntryManager.findEntries(getPeopleBaseDn(), User.class, searchFiler, returnAttributes, 1);
		log.debug("Found {} entries for user {} = {}", entries.size(), ArrayHelper.toString(attributeNames), attributeValue);

		if (entries.size() > 0) {
			return entries.get(0);
		} else {
			return null;
		}
	}

	public User getUserByAttributes(List<CustomAttribute> attributes, boolean andFilter, String... returnAttributes) {
		if (attributes == null) {
			return null;
		}

		log.debug("Getting user information using next attributes '{}'", attributes);

		List<Filter> filters = new ArrayList<Filter>(); 
		for (CustomAttribute attribute : attributes) {
			Filter filter = Filter.createEqualityFilter(attribute.getName(), attribute.getValues());
        	filter.multiValued(attribute.isMultiValued());
			filters.add(filter);
		}

		Filter searchFiler;
		if (filters.size() == 1) {
			searchFiler = filters.get(0);
		} else {
			if (andFilter) {
				searchFiler = Filter.createANDFilter(filters);
			} else {
				searchFiler = Filter.createORFilter(filters);
			}
		}

		List<User> entries = persistenceEntryManager.findEntries(getPeopleBaseDn(), User.class, searchFiler, returnAttributes, 1);
		log.debug("Found '{}' entries for user by next attributes '{}'", entries.size(), attributes);

		if (entries.size() > 0) {
			return entries.get(0);
		} else {
			return null;
		}
	}

    public List<User> getUsersBySample(User user, int limit) {
        log.debug("Getting user by sample");

        List<User> entries = persistenceEntryManager.findEntries(user, limit);
        log.debug("Found '{}' entries", entries.size());

        return entries;
    }

    public User addUserAttributeByUserInum(String userInum, String attributeName, Object attributeValue) {
    	log.debug("Add user attribute by user inum  to LDAP: attributeName = '{}', attributeValue = '{}'", attributeName, attributeValue);

        User user = getUserByInum(userInum);
        if (user == null) {
        	return null;
        }
  
        boolean result = addUserAttribute(user, attributeName, attributeValue);
        if (!result) {
        	// We uses this result in Person Authentication Scripts
        	addUserAttribute(user, attributeName, attributeValue);
        }

        return updateUser(user);
    	
    }

    public User addUserAttribute(String userId, String attributeName, Object attributeValue) {
    	return addUserAttribute(userId, attributeName, attributeValue, null);
    }
    
    public User addUserAttribute(String userId, String attributeName, Object attributeValue, Boolean multiValued) {
        log.debug("Add user attribute to LDAP: attributeName = '{}', attributeValue = '{}'", attributeName, attributeValue);

        User user = getUser(userId);
        if (user == null) {
        	// We uses this result in Person Authentication Scripts
        	return null;
        }
        
        boolean result = addUserAttribute(user, attributeName, attributeValue, multiValued);
        if (!result) {
        	// We uses this result in Person Authentication Scripts
        	return null;
        }

        return updateUser(user);
    }

    public boolean addUserAttribute(User user, String attributeName, Object attributeValue) {
    	return addUserAttribute(user, attributeName, attributeValue, null);
    }

    public boolean addUserAttribute(User user, String attributeName, Object attributeValue, Boolean multiValued) {
    	CustomObjectAttribute customAttribute = getCustomAttribute(user, attributeName);
        if (customAttribute == null) {
        	customAttribute = new CustomObjectAttribute(attributeName, attributeValue);
            user.getCustomAttributes().add(customAttribute);
        } else {
        	List<Object> currentAttributeValues = customAttribute.getValues();

        	List<Object> newAttributeValues = new ArrayList<Object>();
        	newAttributeValues.addAll(currentAttributeValues);

        	if (newAttributeValues.contains(attributeValue)) {
        		return false;
        	} else {
        		newAttributeValues.add(attributeValue);
        	}
        	
        	customAttribute.setValues(newAttributeValues);
        }

        if (multiValued != null) {
        	customAttribute.setMultiValued(multiValued);
        }
        
        return true;
	}

    public User removeUserAttribute(String userId, String attributeName, String attributeValue) {
        log.debug("Remove user attribute from LDAP: attributeName = '{}', attributeValue = '{}'", attributeName, attributeValue);

        User user = getUser(userId);
        if (user == null) {
        	return null;
        }
        
        CustomObjectAttribute customAttribute = getCustomAttribute(user, attributeName);
        if (customAttribute != null) {
        	List<Object> currentAttributeValues = customAttribute.getValues();
        	if (currentAttributeValues.contains(attributeValue)) {

        		List<Object> newAttributeValues = new ArrayList<Object>();
            	newAttributeValues.addAll(currentAttributeValues);
        		if (currentAttributeValues.contains(attributeValue)) {
            		newAttributeValues.remove(attributeValue);
            	} else {
            		return null;
            	}

        		customAttribute.setValues(newAttributeValues);
        	}
        }

		return updateUser(user);
    }

    public User replaceUserAttribute(String userId, String attributeName, String oldAttributeValue, String newAttributeValue) {
    	return replaceUserAttribute(userId, attributeName, oldAttributeValue, newAttributeValue, null);
    }

    public User replaceUserAttribute(String userId, String attributeName, String oldAttributeValue, String newAttributeValue, Boolean multiValued) {
        log.debug("Replace user attribute in LDAP: attributeName = '{}', oldAttributeValue = '{}', newAttributeValue = '{}'", attributeName, oldAttributeValue, newAttributeValue);

        User user = getUser(userId);
        if (user == null) {
        	return null;
        }
        
        CustomObjectAttribute customAttribute = getCustomAttribute(user, attributeName);
        if (customAttribute != null) {
        	List<Object> currentAttributeValues = customAttribute.getValues();
    		List<Object> newAttributeValues = new ArrayList<Object>();
        	newAttributeValues.addAll(currentAttributeValues);

    		if (currentAttributeValues.contains(oldAttributeValue)) {
        		newAttributeValues.remove(oldAttributeValue);
        	}

        	if (!newAttributeValues.contains(newAttributeValue)) {
        		newAttributeValues.add(newAttributeValue);
        	}

        	customAttribute.setValues(newAttributeValues);
        }
        
        if (multiValued != null) {
        	customAttribute.setMultiValued(multiValued);
        }

		return updateUser(user);
    }

	public CustomObjectAttribute getCustomAttribute(User user, String attributeName) {
		for (CustomObjectAttribute customAttribute : user.getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attributeName, customAttribute.getName())) {
				return customAttribute;
			}
		}

		return null;
	}

	public void setCustomAttribute(User user, String attributeName, String attributeValue) {
		CustomObjectAttribute customAttribute = getCustomAttribute(user, attributeName);
		
		if (customAttribute == null) {
			customAttribute = new CustomObjectAttribute(attributeName);
			user.getCustomAttributes().add(customAttribute);
		}
		
		customAttribute.setValue(attributeValue);
	}
//
//    // this method must be called only if app mode = MEMORY, in ldap case it's anyway persisted in ldap.
//    public boolean saveLongLivedToken(String userId, PersistentJwt longLivedToken) {
//        log.debug("Saving long-lived access token: userId = {}", userId);
//        boolean succeed = false;
//
//        User user = getUser(userId);
//        if (user != null) {
//            int nTokens = 0;
//            if (user.getOxAuthPersistentJwt() != null) {
//                nTokens = user.getOxAuthPersistentJwt().length;
//            }
//            nTokens++;
//            String[] persistentJwts = new String[nTokens];
//            if (user.getOxAuthPersistentJwt() != null) {
//                for (int i = 0; i < user.getOxAuthPersistentJwt().length; i++) {
//                    persistentJwts[i] = user.getOxAuthPersistentJwt()[i];
//                }
//            }
//            persistentJwts[nTokens - 1] = longLivedToken.toString();
//
//            user.setOxAuthPersistentJwt(persistentJwts);
//            ldapEntryManager.merge(user);
//            succeed = true;
//        }
//
//        return succeed;
//    }

    public List<User> getUsersWithPersistentJwts() {
        String baseDN = getPeopleBaseDn();
        Filter filter = Filter.createPresenceFilter("jansPersistentJWT");

        return persistenceEntryManager.findEntries(baseDN, User.class, filter);
    }

    public String getDnForUser(String inum) {
		String peopleDn = getPeopleBaseDn();
		if (StringHelper.isEmpty(inum)) {
			return peopleDn;
		}

		return String.format("inum=%s,%s", inum, peopleDn);
	}

	public String getUserInumByDn(String dn) {
		if (StringHelper.isEmpty(dn)) {
			return null;
		}

		String peopleDn = getPeopleBaseDn();
		if (!dn.toLowerCase().endsWith(peopleDn.toLowerCase())) {
			return null;
		}
		String firstDnPart = dn.substring(0, dn.length() - peopleDn.length());
		
		String[] dnParts = firstDnPart.split(",");
		if (dnParts.length == 0) {
			return null;
		}
		
		String userInumPart = dnParts[dnParts.length - 1];
		String[] userInumParts = userInumPart.split("=");
		if ((userInumParts.length == 2) && StringHelper.equalsIgnoreCase(userInumParts[0], "inum")) {
			return userInumParts[1];
		}

		return null;
	}

	public String encodeGeneralizedTime(Date date) {
		String baseDn = getDnForUser(null);
		return persistenceEntryManager.encodeTime(baseDn, date);
	}

	public Date decodeGeneralizedTime(String date) {
		String baseDn = getDnForUser(null);
		return persistenceEntryManager.decodeTime(baseDn, date);
	}

	public abstract List<String> getPersonCustomObjectClassList();

	public abstract String getPeopleBaseDn();
}
