/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.management.InvalidAttributeValueException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.config.oxtrust.AppConfiguration;
import io.jans.model.GluuStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.scim.model.GluuBoolean;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.scim.ScimCustomPerson;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.extensions.ExtensionField;
import io.jans.scim.model.scim2.user.Address;
import io.jans.scim.model.scim2.user.Email;
import io.jans.scim.model.scim2.user.Entitlement;
import io.jans.scim.model.scim2.user.Group;
import io.jans.scim.model.scim2.user.InstantMessagingAddress;
import io.jans.scim.model.scim2.user.Name;
import io.jans.scim.model.scim2.user.PhoneNumber;
import io.jans.scim.model.scim2.user.Photo;
import io.jans.scim.model.scim2.user.Role;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.user.X509Certificate;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.GroupService;
import io.jans.scim.service.PersonService;
import io.jans.scim.service.antlr.scimFilter.ScimFilterParserService;
import io.jans.scim.service.external.ExternalScimService;
import io.jans.scim.util.ServiceUtil;
import io.jans.scim.ws.rs.scim2.GroupWebService;

/**
 * This class holds the most important business logic of the SCIM service for
 * the resource type "User". It's devoted to taking objects of class
 * UserResource, feeding instances of ScimCustomPerson, and do persistence to
 * LDAP. The converse is also done: querying LDAP, and transforming
 * ScimCustomPerson into UserResource
 *
 * @author jgomer
 */
@ApplicationScoped
public class Scim2UserService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5948992380577056420L;

	@Inject
	private Logger log;

	@Inject
	private PersonService personService;

	@Inject
    private UserPersistenceHelper userPersistenceHelper;

	@Inject
	private GroupService groupService;

	@Inject
	private ExternalScimService externalScimService;

	@Inject
	private ServiceUtil serviceUtil;

	@Inject
	private ExtensionService extService;

	@Inject
	private ScimFilterParserService scimFilterParserService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
    AppConfiguration appConfiguration;

	private boolean ldapBackend;

	private String groupEndpointUrl;

	private String[] getComplexMultivaluedAsArray(List items) {

		String array[] = {};

		try {
			if (items != null && items.size() > 0) {
				ObjectMapper mapper = ServiceUtil.getObjectMapper();
				List<String> itemList = new ArrayList<>();

				for (Object item : items) {
                    itemList.add(mapper.writeValueAsString(item));
                }

				array = itemList.toArray(new String[0]);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return array;

	}

	private <T> List<T> getAttributeListValue(ScimCustomPerson source, Class<T> clazz, String attrName) {

		List<T> items = new ArrayList<>();
		try {
			ObjectMapper mapper = ServiceUtil.getObjectMapper();

            for (String attribute : source.getAttributeList(attrName)) {
                T item = mapper.readValue(attribute, clazz);
                items.add(item);
            }
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return items.size() == 0 ? null : items;

	}

	private void transferAttributesToPerson(UserResource res, ScimCustomPerson person) {

		log.debug("transferAttributesToPerson");
		// NOTE: calling person.setAttribute("ATTR", null) is not changing the attribute in LDAP :(

		// Set values trying to follow the order found in BaseScimResource class
		person.setAttribute("excludeExternalId", res.getExternalId());
		person.setCustomAttribute("excludeMetaCreated", res.getMeta().getCreated());
		person.setCustomAttribute("excludeMetaLastMod", res.getMeta().getLastModified());
		// When creating user, location will be set again when having an inum
		person.setCustomAttribute("excludeMetaLocation", res.getMeta().getLocation());

		// Set values trying to follow the order found in UserResource class
		person.setUid(res.getUserName());

		if (res.getName() != null) {
			person.setAttribute("givenName", res.getName().getGivenName());
			person.setAttribute("sn", res.getName().getFamilyName());
			person.setAttribute("middleName", res.getName().getMiddleName());
			person.setAttribute("excludehonorificPrefix", res.getName().getHonorificPrefix());
			person.setAttribute("excludehonorificSuffix", res.getName().getHonorificSuffix());
			person.setAttribute("excludeNameFormatted", res.getName().computeFormattedName());
		}
		person.setAttribute("displayName", res.getDisplayName());

		person.setAttribute("nickname", res.getNickName());
		person.setAttribute("excludeProfileURL", res.getProfileUrl());
		person.setAttribute("excludeTitle", res.getTitle());
		person.setAttribute("excludeUsrTyp", res.getUserType());

		person.setAttribute("preferredLanguage", res.getPreferredLanguage());
		person.setAttribute("locale", res.getLocale());
		person.setAttribute("zoneinfo", res.getTimezone());

		// Why both jsStatus and excludeActive used for active? it's for active being used in filter queries?
		// Also it seems jsStatus can have several values, see org.gluu.model.GluuStatus
		Boolean active = Optional.ofNullable(res.getActive()).orElse(false);
		person.setCustomAttribute("excludeActive", active);
		person.setAttribute("jansStatus",
				active ? GluuStatus.ACTIVE.getValue() : GluuStatus.INACTIVE.getValue());
		person.setUserPassword(res.getPassword());

		person.setAttribute("excludeEmail", getComplexMultivaluedAsArray(res.getEmails()));
		try {
			person = userPersistenceHelper.syncEmailForward(person);
		} catch (Exception e) {
			log.error("Problem syncing emails forward", e);
		}

		person.setAttribute("excludePhoneValue", getComplexMultivaluedAsArray(res.getPhoneNumbers()));
		person.setAttribute("excludeImsValue", getComplexMultivaluedAsArray(res.getIms()));
		person.setAttribute("excludePhotos", getComplexMultivaluedAsArray(res.getPhotos()));
		person.setAttribute("excludeAddres", getComplexMultivaluedAsArray(res.getAddresses()));

		// group membership changes MUST be applied via the "Group" Resource (Section
		// 4.1.2 & 8.7.1 RFC 7643) only

		person.setAttribute("excludeEntitlements", getComplexMultivaluedAsArray(res.getEntitlements()));
		person.setAttribute("excludeRole", getComplexMultivaluedAsArray(res.getRoles()));
		person.setAttribute("excludex509Certificate", getComplexMultivaluedAsArray(res.getX509Certificates()));

		// Pairwise identifiers must not be supplied here... (they are mutability = readOnly)
		transferExtendedAttributesToPerson(res, person);

	}

	/**
	 * Takes all extended attributes found in the SCIM resource and copies them to a
	 * ScimCustomPerson This method is called after validations take place (see
	 * associated decorator for User Service), so all inputs are OK and can go
	 * straight to LDAP with no runtime surprises
	 * 
	 * @param resource
	 *            A SCIM resource used as origin of data
	 * @param person
	 *            a ScimCustomPerson used as destination
	 */
	private void transferExtendedAttributesToPerson(BaseScimResource resource, ScimCustomPerson person) {

		try {
			// Gets all the extended attributes for this resource
			Map<String, Object> extendedAttrs = resource.getCustomAttributes();

			// Iterates over all extensions this type of resource might have
			for (Extension extension : extService.getResourceExtensions(resource.getClass())) {
				Object val = extendedAttrs.get(extension.getUrn());

				if (val != null) {
					// Obtains the attribute/value(s) pairs in the current extension
					Map<String, Object> attrsMap = IntrospectUtil.strObjMap(val);

					for (String attribute : attrsMap.keySet()) {
						Object value = attrsMap.get(attribute);

						if (value == null) {
							// Attribute was unassigned in this resource: drop it from destination too
							log.debug("transferExtendedAttributesToPerson. Flushing attribute {}", attribute);
							person.setAttribute(attribute, (String) null);
						} else {

						    ExtensionField field = extension.getFields().get(attribute);
                            if (field.isMultiValued()) {
                                person.setCustomAttribute(attribute, extService.getAttributeValues(field, (Collection) value, ldapBackend));
                            } else {
                                person.setCustomAttribute(attribute, extService.getAttributeValue(field, value, ldapBackend));
                            }
                            log.debug("transferExtendedAttributesToPerson. Setting attribute '{}' with values {}",
                                    attribute, person.getTypedAttribute(attribute).getDisplayValue());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public void transferAttributesToUserResource(ScimCustomPerson person, UserResource res, String url) {

		log.debug("transferAttributesToUserResource");

		res.setId(person.getInum());
		res.setExternalId(person.getAttribute("excludeExternalId"));

		Meta meta = new Meta();
		meta.setResourceType(ScimResourceUtil.getType(res.getClass()));

		meta.setCreated(person.getAttribute("excludeMetaCreated"));
		if (meta.getCreated() == null) {
			Date date = person.getCreationDate();
			meta.setCreated(date == null ? null : ISODateTimeFormat.dateTime().withZoneUTC().print(date.getTime()));
		}

		meta.setLastModified(person.getAttribute("excludeMetaLastMod"));
		if (meta.getLastModified() == null) {
			Date date = person.getUpdatedAt();
			meta.setLastModified(date == null ? null : ISODateTimeFormat.dateTime().withZoneUTC().print(date.getTime()));
		}

		meta.setLocation(person.getAttribute("excludeMetaLocation"));
		if (meta.getLocation() == null) {
            meta.setLocation(url + "/" + person.getInum());
        }

		res.setMeta(meta);

		// Set values in order of appearance in UserResource class
		res.setUserName(person.getUid());

		Name name = new Name();
		name.setGivenName(person.getGivenName());
		name.setFamilyName(person.getSurname());
		name.setMiddleName(person.getAttribute("middleName"));
		name.setHonorificPrefix(person.getAttribute("excludehonorificPrefix"));
		name.setHonorificSuffix(person.getAttribute("excludehonorificSuffix"));

		String formatted = person.getAttribute("excludeNameFormatted");
		if (formatted == null) { // recomputes the formatted name if absent in LDAP
            name.computeFormattedName();
        } else {
            name.setFormatted(formatted);
        }

		res.setName(name);
		res.setDisplayName(person.getDisplayName());

		res.setNickName(person.getAttribute("nickname"));
		res.setProfileUrl(person.getAttribute("excludeProfileURL"));
		res.setTitle(person.getAttribute("excludeTitle"));
		res.setUserType(person.getAttribute("excludeUsrTyp"));

		res.setPreferredLanguage(person.getPreferredLanguage());
		res.setLocale(person.getAttribute("locale"));
		res.setTimezone(person.getTimezone());

		res.setActive(Boolean.valueOf(person.getAttribute("excludeActive"))
				|| GluuBoolean.getByValue(person.getAttribute("jansStatus")).isBooleanValue());
		res.setPassword(person.getUserPassword());

		res.setEmails(getAttributeListValue(person, Email.class, "excludeEmail"));
        if (res.getEmails() == null) {
            //There can be cases where excludeEmail is not synced with mail attribute....
            List<Email> emails = person.getAttributeList("mail").stream()
                    .map(m -> {
                        Email email = new Email();
                        email.setValue(m);
                        email.setPrimary(false);
                        return email;
                    }).collect(Collectors.toList());
            res.setEmails(emails.size() == 0 ? null : emails);
        }

        res.setPhoneNumbers(getAttributeListValue(person, PhoneNumber.class, "excludePhoneValue"));
		res.setIms(getAttributeListValue(person, InstantMessagingAddress.class, "excludeImsValue"));
		res.setPhotos(getAttributeListValue(person, Photo.class, "excludePhotos"));
		res.setAddresses(getAttributeListValue(person, Address.class, "excludeAddres"));

		List<String> listOfGroups = person.getMemberOf();
		if (listOfGroups != null && listOfGroups.size() > 0) {
			List<Group> groupList = new ArrayList<>();

			for (String groupDN : listOfGroups) {
				try {
					GluuGroup gluuGroup = groupService.getGroupByDn(groupDN);

					Group group = new Group();
					group.setValue(gluuGroup.getInum());
					String reference = groupEndpointUrl + "/" + gluuGroup.getInum();
					group.setRef(reference);
					group.setDisplay(gluuGroup.getDisplayName());
					group.setType(Group.Type.DIRECT); // Only support direct membership: see section 4.1.2 of RFC 7644

					groupList.add(group);
				} catch (Exception e) {
					log.warn(
							"transferAttributesToUserResource. Group with dn {} could not be added to User Resource. {}",
							groupDN, person.getUid());
					log.error(e.getMessage(), e);
				}
			}
			if (groupList.size() > 0) {
                res.setGroups(groupList);
            }
		}

		res.setEntitlements(getAttributeListValue(person, Entitlement.class, "excludeEntitlements"));
		res.setRoles(getAttributeListValue(person, Role.class, "excludeRole"));
		res.setX509Certificates(getAttributeListValue(person, X509Certificate.class, "excludex509Certificate"));

		res.setPairwiseIdentifiers(person.getPpid());

		transferExtendedAttributesToResource(person, res);

	}

	private void transferExtendedAttributesToResource(ScimCustomPerson person, BaseScimResource resource) {

		log.debug("transferExtendedAttributesToResource of type {}", ScimResourceUtil.getType(resource.getClass()));

		// Gets the list of extensions associated to the resource passed. In practice,
		// this will be at most a singleton list
		List<Extension> extensions = extService.getResourceExtensions(resource.getClass());

		// Iterate over every extension to copy extended attributes from person to
		// resource
		for (Extension extension : extensions) {
			Map<String, ExtensionField> fields = extension.getFields();
			// Create empty map to store the values of the extended attributes found for
			// current extension in object person
			Map<String, Object> map = new HashMap<>();

			log.debug("transferExtendedAttributesToResource. Revising attributes of extension '{}'",
					extension.getUrn());

			// Iterate over every attribute part of this extension
			for (String attr : fields.keySet()) {
				// Gets the values associated to this attribute that were found in LDAP
				String values[] = person.getAttributes(attr);

				if (values != null) {

					log.debug("transferExtendedAttributesToResource. Copying to resource the value(s) for attribute '{}'",
							attr);
					ExtensionField field = fields.get(attr);
					List<Object> convertedValues = extService.convertValues(field, values, ldapBackend);

					if (convertedValues.size() > 0) {
						map.put(attr, field.isMultiValued() ? convertedValues : convertedValues.get(0));
					}
				}
			}
			// Stores all extended attributes (with their values) in the resource object
			if (map.size() > 0) {
				resource.addCustomAttributes(extension.getUrn(), map);
			}
		}
		for (String urn : resource.getCustomAttributes().keySet()) {
            resource.getSchemas().add(urn);
        }

	}

	private void writeCommonName(ScimCustomPerson person) {

		if (StringUtils.isNotEmpty(person.getGivenName()) && StringUtils.isNotEmpty(person.getSurname())) {
            person.setCommonName(person.getGivenName() + " " + person.getSurname());
        }

	}

	private void assignComputedAttributesToPerson(ScimCustomPerson person) {

		String inum = personService.generateInumForNewPerson();
		String dn = personService.getDnForPerson(inum);

		person.setInum(inum);
		person.setDn(dn);
		writeCommonName(person);

	}

	/**
	 * Inserts a new user in LDAP based on the SCIM Resource passed
	 * 
	 * @param user
	 *            A UserResource object with all info as received by the web service
	 * @param url Base URL associated to user resources in SCIM (eg. .../scim/v2/Users)
	 * @throws Exception In case of unexpected error
	 */
	public void createUser(UserResource user, String url) throws Exception {

		String userName = user.getUserName();
		log.info("Preparing to create user {}", userName);

		// There is no need to check attributes mutability in this case as there are no
		// original attributes
		// (the resource does not exist yet)
		ScimCustomPerson gluuPerson = new ScimCustomPerson();
		transferAttributesToPerson(user, gluuPerson);
		assignComputedAttributesToPerson(gluuPerson);

		String location = url + "/" + gluuPerson.getInum();
		gluuPerson.setAttribute("excludeMetaLocation", location);

		log.info("Persisting user {}", userName);
		userPersistenceHelper.addCustomObjectClass(gluuPerson);

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimCreateUserMethods(gluuPerson);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}

			userPersistenceHelper.addPerson(gluuPerson);
			// Copy back to user the info from gluuPerson
			transferAttributesToUserResource(gluuPerson, user, url);
			externalScimService.executeScimPostCreateUserMethods(gluuPerson);
		} else {
            userPersistenceHelper.addPerson(gluuPerson);
			user.getMeta().setLocation(location);
			// We are ignoring the id value received (user.getId())
			user.setId(gluuPerson.getInum());
		}

	}

	public UserResource updateUser(String id, UserResource user, String url) throws InvalidAttributeValueException {

		ScimCustomPerson gluuPerson = userPersistenceHelper.getPersonByInum(id); // This is never null (see decorator involved)
		UserResource tmpUser = new UserResource();

		transferAttributesToUserResource(gluuPerson, tmpUser, url);

		long now = System.currentTimeMillis();
		tmpUser.getMeta().setLastModified(ISODateTimeFormat.dateTime().withZoneUTC().print(now));

		tmpUser = (UserResource) ScimResourceUtil.transferToResourceReplace(user, tmpUser,
				extService.getResourceExtensions(user.getClass()));
		replacePersonInfo(gluuPerson, tmpUser, url);

		return tmpUser;

	}

	public void replacePersonInfo(ScimCustomPerson gluuPerson, UserResource user, String url) {
		transferAttributesToPerson(user, gluuPerson);
		writeCommonName(gluuPerson);

		log.debug("replacePersonInfo. Updating person info in LDAP");
		userPersistenceHelper.addCustomObjectClass(gluuPerson);

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimUpdateUserMethods(gluuPerson);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}

			userPersistenceHelper.updatePerson(gluuPerson);
			// Copy back to user the info from gluuPerson
			transferAttributesToUserResource(gluuPerson, user, url);
			externalScimService.executeScimPostUpdateUserMethods(gluuPerson);
		} else {
            userPersistenceHelper.updatePerson(gluuPerson);
		}

	}

	public void deleteUser(ScimCustomPerson gluuPerson) throws Exception {

		String dn = gluuPerson.getDn();
		if (gluuPerson.getMemberOf() != null && gluuPerson.getMemberOf().size() > 0) {
			log.info("Removing user {} from groups", gluuPerson.getUid());
			userPersistenceHelper.deleteUserFromGroup(gluuPerson, dn);
		}
		log.info("Removing user entry {}", dn);

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimDeleteUserMethods(gluuPerson);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}
		}

		userPersistenceHelper.removePerson(gluuPerson);

		if (externalScimService.isEnabled())
			externalScimService.executeScimPostDeleteUserMethods(gluuPerson);

	}

	public PagedResult<BaseScimResource> searchUsers(String filter, String sortBy, SortOrder sortOrder, int startIndex,
			int count, String url, int maxCount) throws Exception {

		Filter ldapFilter = scimFilterParserService.createFilter(filter, Filter.createPresenceFilter("inum"), UserResource.class);
		log.info("Executing search for users using: ldapfilter '{}', sortBy '{}', sortOrder '{}', startIndex '{}', count '{}'",
				ldapFilter.toString(), sortBy, sortOrder.getValue(), startIndex, count);

		PagedResult<ScimCustomPerson> list = ldapEntryManager.findPagedEntries(personService.getDnForPerson(null),
				ScimCustomPerson.class, ldapFilter, null, sortBy, sortOrder, startIndex - 1, count, maxCount);
		List<BaseScimResource> resources = new ArrayList<>();
		
		if (externalScimService.isEnabled() && !externalScimService.executeScimPostSearchUsersMethods(list)) {
			throw new WebApplicationException("Failed to execute SCIM script successfully", Status.PRECONDITION_FAILED);
		}

		for (ScimCustomPerson person : list.getEntries()) {
			UserResource scimUsr = new UserResource();
			transferAttributesToUserResource(person, scimUsr, url);
			resources.add(scimUsr);
		}
		log.info("Found {} matching entries - returning {}", list.getTotalEntriesCount(), list.getEntries().size());

		PagedResult<BaseScimResource> result = new PagedResult<>();
		result.setEntries(resources);
		result.setTotalEntriesCount(list.getTotalEntriesCount());

		return result;

	}

	// See: https://github.com/GluuFederation/oxTrust/issues/800
	public void removePPIDsBranch(String dn) {
		try {
			ldapEntryManager.removeRecursively(String.format("ou=pairwiseIdentifiers,%s", dn));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@PostConstruct
    private void init() {
        ldapBackend = scimFilterParserService.isLdapBackend();
		groupEndpointUrl = appConfiguration.getBaseEndpoint() + GroupWebService.class.getAnnotation(Path.class).value();
    }

}
