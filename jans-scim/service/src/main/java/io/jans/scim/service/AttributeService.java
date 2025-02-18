/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import io.jans.model.JansAttribute;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.scim.model.JansCustomAttribute;
import io.jans.scim.model.conf.AppConfiguration;
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

	/**
	 * Get SCIM related attributes
	 * 
	 * @return Attribute
	 * @throws Exception Unexpected failure
	 */
	public List<JansAttribute> getSCIMRelatedAttributes() throws Exception {
		List<JansAttribute> attributes = getAllAttributes();

		List<JansAttribute> result = new ArrayList<JansAttribute>();
		for (JansAttribute attribute : attributes) {
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
	protected List<JansAttribute> getAllAtributesImpl(String baseDn) {
		List<JansAttribute> attributeList = persistenceEntryManager.findEntries(baseDn, JansAttribute.class, null);
		String customOrigin = getCustomOrigin();
		for (JansAttribute attribute : attributeList) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}

		return attributeList;
	}

	@Override
	protected BaseCacheService getCacheService() {
		return cacheService;
	}

	public void applyMetaData(List<JansCustomAttribute> customAttributes) {
		if ((customAttributes == null) || (customAttributes.size() == 0)) {
			return;
		}
		
		Map<String, JansAttribute> allAttributesMap = getAllAttributesMap();
		for (JansCustomAttribute customAttribute : customAttributes) {
			String attributeName = StringHelper.toLowerCase(customAttribute.getName());
			
			JansAttribute attribute = allAttributesMap.get(attributeName);
			if (attribute != null) {
				customAttribute.setMetadata(attribute);
			}
		}
	}

	public void applyMultiValued(List<CustomObjectAttribute> customAttributes) {
		if ((customAttributes == null) || (customAttributes.size() == 0)) {
			return;
		}
		
		Map<String, JansAttribute> allAttributesMap = getAllAttributesMap();
		for (CustomObjectAttribute customAttribute : customAttributes) {
			String attributeName = StringHelper.toLowerCase(customAttribute.getName());
			
			JansAttribute attribute = allAttributesMap.get(attributeName);
			if (attribute != null) {
				boolean multiValued = Boolean.TRUE.equals(attribute.getOxMultiValuedAttribute());
				customAttribute.setMultiValued(multiValued);
			}
		}
	}

}
