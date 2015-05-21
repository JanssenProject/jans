/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;

/**
 * Provides operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@Scope(ScopeType.STATELESS)
@Name("deviceRegistrationService")
@AutoCreate
public class DeviceRegistrationService {

	@In
	private LdapEntryManager ldapEntryManager;

	@Logger
	private Log log;

	public void addBranch(final String userName) {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("device_registration");
		branch.setDn(getDnForResourceSet(null));

		ldapEntryManager.persist(branch);
	}

	public List<DeviceRegistration> findUserDeviceRegistrations(String appId, String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addUserDeviceRegistration(String userName, String appId, DeviceRegistration deviceRegistration) {
		// TODO Auto-generated method stub

	}

	public void updateDeviceRegistration(DeviceRegistration usedDeviceRegistration) {
		// TODO Auto-generated method stub

	}

	// /**
	// * Add new resource set description entry
	// *
	// * @param resourceSet resourceSet
	// */
	// public void addResourceSet(ResourceSet resourceSet) {
	// validate(resourceSet);
	// ldapEntryManager.persist(resourceSet);
	// }
	//
	// public void validate(ResourceSet resourceSet) {
	// Preconditions.checkArgument(StringUtils.isNotBlank(resourceSet.getName()),
	// "Name is required for resource set.");
	// Preconditions.checkArgument(resourceSet.getScopes() != null &&
	// !resourceSet.getScopes().isEmpty(),
	// "Scope must be specified for resource set.");
	// }
	//
	// /**
	// * Update resource set description entry
	// *
	// * @param resourceSet resourceSet
	// */
	// public void updateResourceSet(ResourceSet resourceSet) {
	// validate(resourceSet);
	// ldapEntryManager.merge(resourceSet);
	// }
	//
	// /**
	// * Remove resource set description entry
	// *
	// * @param resourceSet resourceSet
	// */
	// public void remove(ResourceSet resourceSet) {
	// ldapEntryManager.remove(resourceSet);
	// }
	//
	// public void remove(List<ResourceSet> resourceSet) {
	// for (ResourceSet resource : resourceSet) {
	// remove(resource);
	// }
	// }
	//
	// /**
	// * Get all resource set descriptions
	// *
	// * @return List of resource set descriptions
	// */
	// public List<ResourceSet> getAllResourceSets(String...
	// ldapReturnAttributes) {
	// return ldapEntryManager.findEntries(getBaseDnForResourceSet(),
	// ResourceSet.class, ldapReturnAttributes, null);
	// }
	//
	// /**
	// * Get all resource set descriptions
	// *
	// * @return List of resource set descriptions
	// */
	// public List<ResourceSet> getResourceSetsByAssociatedClient(String
	// p_associatedClientDn) {
	// try {
	// if (StringUtils.isNotBlank(p_associatedClientDn)) {
	// final Filter filter =
	// Filter.create(String.format("&(oxAssociatedClient=%s)",
	// p_associatedClientDn));
	// return ldapEntryManager.findEntries(getBaseDnForResourceSet(),
	// ResourceSet.class, filter);
	// }
	// } catch (Exception e) {
	// log.error(e.getMessage(), e);
	// }
	// return Collections.emptyList();
	// }
	//
	// /**
	// * Get resource set descriptions by example
	// *
	// * @param resourceSet ResourceSet
	// * @return ResourceSet which conform example
	// */
	// public List<ResourceSet> findResourceSets(ResourceSet resourceSet) {
	// return ldapEntryManager.findEntries(resourceSet);
	// }
	//
	// public boolean containsBranch() {
	// return ldapEntryManager.contains(SimpleBranch.class,
	// getDnForResourceSet(null));
	// }
	//
	// /**
	// * Check if LDAP server contains resource set description with specified
	// attributes
	// *
	// * @return True if resource set description with specified attributes
	// exist
	// */
	// public boolean containsResourceSet(ResourceSet resourceSet) {
	// return ldapEntryManager.contains(resourceSet);
	// }
	//
	// /**
	// * Get resource set description by DN
	// *
	// * @param dn Resource set description DN
	// * @return Resource set description
	// */
	// public ResourceSet getResourceSetByDn(String dn) {
	// return ldapEntryManager.find(ResourceSet.class, dn);
	// }

	/**
	 * Build DN string for resource set description
	 */
	public String getDnForResourceSet(String oxId) {
		if (StringHelper.isEmpty(oxId)) {
			return getBaseDnForResourceSet();
		}
		return String.format("oxId=%s,%s", oxId, getBaseDnForResourceSet());
	}

	public String getBaseDnForResourceSet() {
		final String umaBaseDn = ConfigurationFactory.getBaseDn().getUmaBase(); // "ou=uma,o=@!1111,o=gluu"
		return String.format("ou=resource_sets,%s", umaBaseDn);
	}

	/**
	 * Get ResourceSetService instance
	 *
	 * @return ResourceSetService instance
	 */
	public static DeviceRegistrationService instance() {
		return (DeviceRegistrationService) Component.getInstance(DeviceRegistrationService.class);
	}

}
