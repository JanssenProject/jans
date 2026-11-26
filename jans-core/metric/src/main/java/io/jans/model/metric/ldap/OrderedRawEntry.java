/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.ldap;

import io.jans.orm.annotation.DataEntry;

/** Raw metric projection for keyset paging in chronological order, including timestamp ties. */
@DataEntry(sortBy = { "endDate", "id" }, sortByName = { "jansEndDate", "uniqueIdentifier" })
public class OrderedRawEntry extends MetricDataEntry {
}
