package io.jans.orm.search.filter;
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

import java.util.LinkedList;
import java.util.List;

import io.jans.orm.util.StringHelper;

/**
 * Filter processor
 *
 * @author Yuriy Movchan Date: 03/18/2021
 */
public class FilterProcessor {

	public static final Filter OBJECT_CLASS_EQUALITY_FILTER = Filter.createEqualityFilter("objectClass", null);
	public static final Filter OBJECT_CLASS_PRESENCE_FILTER = Filter.createPresenceFilter("objectClass");

	public Filter excludeFilter(Filter genericFilter, Filter... excludeFilters) {
		if (genericFilter == null) {
			return null;
		}

		FilterType type = genericFilter.getType();
		if (FilterType.RAW == type) {
			return genericFilter;
		}

		Filter[] filters = genericFilter.getFilters();
		if (filters != null) {
			List<Filter> resultFilters = new LinkedList<>();
			for (Filter filter : filters) {
				Filter resultFilter = excludeFilter(filter, excludeFilters);
				if (resultFilter != null) {
					resultFilters.add(resultFilter);
				}
			}
			if (resultFilters.size() == 0) {
				return null;
			}

			Filter resultFilter = genericFilter.clone();
			resultFilter.setFilters(resultFilters.toArray(new Filter[0]));
			
			return resultFilter;
		}

		// Check if current filter conform filter specified in excludeFilter
		for (Filter excludeFilter : excludeFilters) {
			boolean typeMatch = (excludeFilter.getType() == null) || (excludeFilter.getType() == type);
			boolean nameMatch = StringHelper.isEmpty(excludeFilter.getAttributeName()) || StringHelper.equalsIgnoreCase(excludeFilter.getAttributeName(), genericFilter.getAttributeName());
			boolean valueMatch = StringHelper.isEmptyString(excludeFilter.getAssertionValue())|| excludeFilter.getAssertionValue().equals(genericFilter.getAssertionValue());
	
			if (typeMatch && nameMatch && valueMatch) {
				return null;
			}
		}

		return genericFilter;
	}

}