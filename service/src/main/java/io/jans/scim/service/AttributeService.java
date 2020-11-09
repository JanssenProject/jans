/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import io.jans.scim.model.conf.AppConfiguration;
import io.jans.model.GluuAttribute;
import io.jans.scim.service.cdi.event.Events;
import io.jans.service.BaseCacheService;
import io.jans.util.StringHelper;

/**
 * Provides operations with attributes
 * 
 * @author Yuriy Movchan Date: 10.13.2010
 */
@ApplicationScoped
public class AttributeService extends io.jans.service.AttributeService {

	private static final long serialVersionUID = 8223624816948822765L;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private OrganizationService organizationService;

	@Inject
	@Any
	private Event<Events> event;

	public static final String CUSTOM_ATTRIBUTE_OBJECTCLASS_PREFIX = "ox-";

	/**
	 * Get SCIM related attributes
	 * 
	 * @return Attribute
	 */
	public List<GluuAttribute> getSCIMRelatedAttributes() throws Exception {
		List<GluuAttribute> attributes = getAllAttributes();

		List<GluuAttribute> result = new ArrayList<GluuAttribute>();
		for (GluuAttribute attribute : attributes) {
			boolean isEmpty = attribute.getScimCustomAttr() == null;
			if (!isEmpty) {
				if ((attribute.getScimCustomAttr() != null) && attribute.getScimCustomAttr()) {
					result.add(attribute);
				}
			}
		}
		return result;
	}

	public String getDnForAttribute(String inum) {
		String organizationDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=attributes,%s", organizationDn);
		}

		return String.format("inum=%s,ou=attributes,%s", inum, organizationDn);
	}

	/**
	 * Return current custom origin
	 * 
	 * @return Current custom origin
	 */
	public String getCustomOrigin() {
		return appConfiguration.getPersonCustomObjectClass();
	}

	@Override
	protected List<GluuAttribute> getAllAtributesImpl(String baseDn) {
		List<GluuAttribute> attributeList = persistenceEntryManager.findEntries(baseDn, GluuAttribute.class, null);
		String customOrigin = getCustomOrigin();
		for (GluuAttribute attribute : attributeList) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}

		return attributeList;
	}

	@Override
	protected BaseCacheService getCacheService() {
		return cacheService;
	}

}
