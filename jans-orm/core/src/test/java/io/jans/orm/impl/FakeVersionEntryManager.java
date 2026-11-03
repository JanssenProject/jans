/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PersistenceMetadata;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.search.filter.Filter;

/**
 * Minimal, pure in-memory {@link BaseEntryManager} fixture for unit-testing the shared
 * {@code merge}/{@code updateWithVersion} logic -- no live DB, no backend module dependency.
 * Does not override {@link #mergeWithVersion} so it exercises the default (unsupported) hook.
 */
class FakeVersionEntryManager extends BaseEntryManager<PersistenceOperationService> {

	final Map<String, List<AttributeData>> storage = new HashMap<>();

	List<AttributeDataModification> lastMergeModifications;
	String lastMergeDn;

	@Override
	protected boolean isSupportForceUpdate() {
		return true;
	}

	@Override
	protected void persist(String dn, String[] objectClasses, List<AttributeData> attributes, Integer expiration) {
		storage.put(dn, new ArrayList<>(attributes));
	}

	@Override
	public Void merge(Object entry) {
		return merge(entry, false, false, null);
	}

	@Override
	protected <T> void updateMergeChanges(String baseDn, T entry, boolean isConfigurationUpdate, Class<?> entryClass,
			Map<String, AttributeData> attributesFromLdapMap, List<AttributeDataModification> attributeDataModifications,
			boolean forceUpdate) {
		// No-op for this fixture
	}

	@Override
	protected void merge(String dn, String[] objectClasses, List<AttributeDataModification> attributeDataModifications,
			Integer expiration) {
		this.lastMergeDn = dn;
		this.lastMergeModifications = attributeDataModifications;
	}

	@Override
	public <T> void removeByDn(String dn, String[] objectClasses) {
		storage.remove(dn);
	}

	@Override
	public <T> void removeRecursivelyFromDn(String primaryKey, String[] objectClasses) {
		storage.remove(primaryKey);
	}

	@Override
	protected <T> boolean contains(String baseDN, String[] objectClasses, Class<T> entryClass,
			List<PropertyAnnotation> propertiesAnnotations, Filter filter, String[] ldapReturnAttributes) {
		return storage.containsKey(baseDN);
	}

	@Override
	protected List<AttributeData> find(String dn, String[] objectClasses, Map<String, PropertyAnnotation> propertiesAnnotationsMap,
			String... attributes) {
		List<AttributeData> result = storage.get(dn);
		return result == null ? new ArrayList<>() : new ArrayList<>(result);
	}

	@Override
	protected Object getNativeDateAttributeValue(Date dateValue) {
		return dateValue;
	}

	@Override
	protected Date decodeTime(String date) {
		return null;
	}

	@Override
	protected String encodeTime(Date date) {
		return null;
	}

	@Override
	@Deprecated
	public boolean authenticate(String primaryKey, String password) {
		return false;
	}

	@Override
	public <T> boolean authenticate(String primaryKey, Class<T> entryClass, String password) {
		return false;
	}

	@Override
	public <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password) {
		return false;
	}

	@Override
	public void remove(Object entry) {
		// Not needed by these tests
	}

	@Override
	public <T> int remove(String primaryKey, Class<T> entryClass, Filter filter, int count) {
		return 0;
	}

	@Override
	public boolean hasBranchesSupport(String primaryKey) {
		return false;
	}

	@Override
	public boolean hasExpirationSupport(String primaryKey) {
		return false;
	}

	@Override
	public String getPersistenceType() {
		return "test";
	}

	@Override
	public String getPersistenceType(String primaryKey) {
		return "test";
	}

	@Override
	public PersistenceMetadata getPersistenceMetadata(String primaryKey) {
		return null;
	}

	@Override
	public Map<String, Map<String, AttributeType>> getTableColumnsMap() {
		return new HashMap<>();
	}

	@Override
	public Date decodeTime(String primaryKey, String date) {
		return null;
	}

	@Override
	public String encodeTime(String primaryKey, Date date) {
		return null;
	}

	@Override
	public PersistenceOperationService getOperationService() {
		return null;
	}

	@Override
	public io.jans.orm.PersistenceEntryManager getPersistenceEntryManager(String persistenceType) {
		return this;
	}

	@Override
	public void addDeleteSubscriber(DeleteNotifier subscriber) {
		// Not needed by these tests
	}

	@Override
	public void removeDeleteSubscriber(DeleteNotifier subscriber) {
		// Not needed by these tests
	}

	@Override
	@Deprecated
	public List<AttributeData> exportEntry(String dn) {
		return new ArrayList<>();
	}

	@Override
	public <T> List<AttributeData> exportEntry(String dn, String objectClass) {
		return new ArrayList<>();
	}

	@Override
	public <T> int countEntries(String primaryKey, Class<T> entryClass, Filter filter) {
		return 0;
	}

	@Override
	public <T> int countEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope) {
		return 0;
	}

	@Override
	public <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope,
			String[] ldapReturnAttributes, BatchOperation<T> batchOperation, int start, int count, int chunkSize) {
		return new ArrayList<>();
	}

	@Override
	public <T> PagedResult<T> findPagedEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes,
			String sortBy, SortOrder sortOrder, int start, int count, int chunkSize) {
		return null;
	}

	@Override
	public boolean destroy() {
		return true;
	}

}

/**
 * Fixture variant that overrides the per-backend {@code mergeWithVersion} hook with a
 * configurable result, to exercise {@code updateWithVersion}'s success / stale-version paths.
 */
class VersioningFakeVersionEntryManager extends FakeVersionEntryManager {

	boolean mergeWithVersionResult = true;

	String lastVersionAttributeName;
	Object lastExpectedVersionValue;
	Object lastNewVersionValue;

	@Override
	protected boolean mergeWithVersion(String dn, String[] objectClasses, List<AttributeDataModification> attributeDataModifications,
			Integer expiration, String versionAttributeName, Object expectedVersionValue, Object newVersionValue) {
		this.lastMergeDn = dn;
		this.lastMergeModifications = attributeDataModifications;
		this.lastVersionAttributeName = versionAttributeName;
		this.lastExpectedVersionValue = expectedVersionValue;
		this.lastNewVersionValue = newVersionValue;

		return mergeWithVersionResult;
	}

}
