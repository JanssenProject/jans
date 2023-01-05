/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

/**
 * Sort attribute name
 *
 * @author Yuriy Movchan Date: 04/22/2021
 */
public class Sort {
    
	private final String name;
	private SortOrder sortOrder;

    public Sort(String name) {
        this.name = name;
    }

    public Sort(String name, SortOrder sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public final String getName() {
        return name;
    }

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public static Sort desc(String name) {
		return new Sort(name, SortOrder.DESCENDING);
	}

	public static Sort asc(String name) {
		return new Sort(name, SortOrder.ASCENDING);
	}

	public static Sort def(String name) {
		return new Sort(name, SortOrder.DEFAULT);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sortOrder == null) ? 0 : sortOrder.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sort other = (Sort) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sortOrder != other.sortOrder)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Sort [name=" + name + ", sortOrder=" + sortOrder + "]";
	}

}
