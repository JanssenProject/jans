/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.hybrid.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.exception.KeyConversionException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.impl.BaseEntryManager;
import io.jans.orm.impl.GenericKeyConverter;
import io.jans.orm.impl.model.ParsedKey;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

/**
 * Hybrid Entry Manager
 *
 * @author Yuriy Movchan Date: 07/10/2019
 */
public class HybridEntryManager extends BaseEntryManager<HybridPersistenceOperationService> implements Serializable {

    private static final long serialVersionUID = -1544664410881103105L;

    private static final Logger LOG = LoggerFactory.getLogger(HybridEntryManager.class);

    private static final GenericKeyConverter KEY_CONVERTER = new GenericKeyConverter();

	private Properties mappingProperties;
	private HashMap<String, PersistenceEntryManager> persistenceEntryManagers;
	private HybridPersistenceOperationService operationService;

	private PersistenceEntryManager defaultPersistenceEntryManager;
	private HashMap<String, PersistenceEntryManager> baseNameToEntryManagerMapping;

    public HybridEntryManager() {
    }

    public HybridEntryManager(Properties mappingProperties, HashMap<String, PersistenceEntryManager> persistenceEntryManagers, HybridPersistenceOperationService operationService) {
    	this.mappingProperties = mappingProperties;
    	this.persistenceEntryManagers = persistenceEntryManagers;
    	this.operationService = operationService;

    	init();
	}

    protected void init() {
        String defaultPersistenceType = mappingProperties.getProperty("storage.default", null);
        if (StringHelper.isEmpty(defaultPersistenceType) || (persistenceEntryManagers.get(defaultPersistenceType) == null)) {
            throw new ConfigurationException("Default persistence type is not defined!");
        }
        this.defaultPersistenceEntryManager = persistenceEntryManagers.get(defaultPersistenceType);

        this.baseNameToEntryManagerMapping = new HashMap<String, PersistenceEntryManager>();
        for (Entry<String, PersistenceEntryManager> persistenceTypeEntry : persistenceEntryManagers.entrySet()) {
        	String mapping = mappingProperties.getProperty(String.format("storage.%s.mapping", persistenceTypeEntry.getKey()), "");
            String[] baseNames = StringHelper.split(mapping, ",");
            for (String baseName : baseNames) {
            	baseNameToEntryManagerMapping.put(baseName, persistenceTypeEntry.getValue());
            }
        }
    }

    @Override
    public void addDeleteSubscriber(DeleteNotifier subscriber) {
        if (this.persistenceEntryManagers == null) {
            return;
        }

        for (PersistenceEntryManager persistenceEntryManager : persistenceEntryManagers.values()) {
    		persistenceEntryManager.addDeleteSubscriber(subscriber);
        }
    }

    @Override
    @Deprecated
    public boolean authenticate(String bindDn, String password) {
    	return authenticate(bindDn, null, password);
    }

    @Override
    public <T> boolean authenticate(String bindDn, Class<T> entryClass, String password) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(bindDn);
    	return persistenceEntryManager.authenticate(bindDn, entryClass, password);
    }

    @Override
    public <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.authenticate(baseDN, entryClass, userName, password);
    }

    @Override
	public boolean contains(Object entry) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
    	return persistenceEntryManager.contains(entry);
	}

    @Override
	public <T> boolean contains(String primaryKey, Class<T> entryClass) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
		return persistenceEntryManager.contains(primaryKey, entryClass);
	}
	@Override
	public <T> boolean contains(String baseDN, Class<T> entryClass, Filter filter) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.contains(baseDN, entryClass, filter);
	}

	@Override
    public <T> int countEntries(Object entry) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
    	return persistenceEntryManager.countEntries(entry);
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.countEntries(baseDN, entryClass, filter);
    }

	@Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.countEntries(baseDN, entryClass, filter, scope);
    }

	@Override
    public Date decodeTime(String baseDN, String date) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.decodeTime(baseDN, date);
    }

    @Override
    public boolean destroy() {
        if (this.persistenceEntryManagers == null) {
            return true;
        }

        boolean result = true;
        for (PersistenceEntryManager persistenceEntryManager : persistenceEntryManagers.values()) {
        	try {
        		result &= persistenceEntryManager.destroy();
        	} catch (Exception ex) {
        		LOG.error("Faild to destroy Persistence Entry Manager", ex);
        	}
        }
        
        return result;
    }

	@Override
    public String encodeTime(String baseDN, Date date) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.encodeTime(baseDN, date);
    }

	@Override
    public List<AttributeData> exportEntry(String dn) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	return persistenceEntryManager.exportEntry(dn);
    }

	@Override
	public <T> List<AttributeData> exportEntry(String dn, String objectClass) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
		return persistenceEntryManager.exportEntry(dn, objectClass);
	}

	@Override
	public <T> void importEntry(String dn, Class<T> entryClass, List<AttributeData> data) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public <T> T find(Class<T> entryClass, Object primaryKey) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
    	return persistenceEntryManager.find(entryClass, primaryKey);
	}

	@Override
	public <T> T find(Object primaryKey, Class<T> entryClass, String[] ldapReturnAttributes) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
    	return persistenceEntryManager.find(primaryKey, entryClass, ldapReturnAttributes);
	}

    @Override
	public <T> List<T> findEntries(Object entry) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
		return persistenceEntryManager.findEntries(entry);
	}

    @Override
	public <T> List<T> findEntries(Object entry, int count) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
		return persistenceEntryManager.findEntries(entry, count);
	}

    @Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findEntries(baseDN, entryClass, filter);
	}

    @Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, int count) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findEntries(baseDN, entryClass, filter, count);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
			BatchOperation<T> batchOperation, int start, int count, int chunkSize) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findEntries(baseDN, entryClass, filter, scope, ldapReturnAttributes, batchOperation, start, count,
				chunkSize);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
			int start, int count, int chunkSize) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findEntries(baseDN, entryClass, filter, scope, ldapReturnAttributes, start, count, chunkSize);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findEntries(baseDN, entryClass, filter, ldapReturnAttributes);
	}

    @Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, int count) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findEntries(baseDN, entryClass, filter, ldapReturnAttributes, count);
	}

    @Override
    public <T> PagedResult<T> findPagedEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
            SortOrder sortOrder, int start, int count, int chunkSize) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
		return persistenceEntryManager.findPagedEntries(baseDN, entryClass, filter, ldapReturnAttributes, sortBy,
	            sortOrder, start, count, chunkSize);
    }

	@Override
	public boolean hasBranchesSupport(String dn) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	return persistenceEntryManager.hasBranchesSupport(dn);
	}

	@Override
	public boolean hasExpirationSupport(String dn) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	return persistenceEntryManager.hasExpirationSupport(dn);
	}

	@Override
	public String getPersistenceType() {
		return HybridEntryManagerFactory.PERSISTENCE_TYPE;
	}

    @Override
	public String getPersistenceType(String primaryKey) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
		return persistenceEntryManager.getPersistenceType(primaryKey);
	}

	@Override
	public PersistenceEntryManager getPersistenceEntryManager(String persistenceType) {
		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagers.get(persistenceType);
		if (persistenceEntryManager != null) {
			return persistenceEntryManager;
		}
		
		if (HybridEntryManagerFactory.PERSISTENCE_TYPE.equals(persistenceType)) {
			return this;
		}
		
		return null;
	}

    private PersistenceEntryManager getPersistenceEntryManagerByKey(String key) {
        if ("_".equals(key)) {
            return defaultPersistenceEntryManager;
        }

        String[] baseNameParts = key.split("_");
        if (ArrayHelper.isEmpty(baseNameParts)) {
            throw new KeyConversionException("Failed to determine base key part!");
        }

        PersistenceEntryManager persistenceEntryManager = baseNameToEntryManagerMapping.get(baseNameParts[0]);
        if (persistenceEntryManager != null) {
            return persistenceEntryManager;
        }

        return defaultPersistenceEntryManager;
    }

    private PersistenceEntryManager getEntryManagerForDn(Object baseDn) {
        if (StringHelper.isEmptyString(baseDn)) {
        	throw new MappingException("Entry DN is null");
        }
        
        return getEntryManagerForDn(baseDn.toString());
	}

    private PersistenceEntryManager getEntryManagerForDn(String baseDn) {
        if (StringHelper.isEmpty(baseDn)) {
        	throw new MappingException("Entry DN is null");
        }

        ParsedKey parsedKey = KEY_CONVERTER.convertToKey(baseDn);
		return getPersistenceEntryManagerByKey(parsedKey.getKey());
	}

	public HybridPersistenceOperationService getOperationService() {
        return operationService;
    }

    @Override
    public Void merge(Object entry) {
		Class<?> entryClass = entry.getClass();
		Object dnValue = getDNValue(entry, entryClass);

		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
    	return persistenceEntryManager.merge(entry);
    }

	@Override
    public void persist(Object entry) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
    	persistenceEntryManager.persist(entry);
	}

	@Override
    public void remove(Object entry) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
    	persistenceEntryManager.remove(entry);
    }

	@Override
	public <T> void removeByDn(String primaryKey, String[] objectClasses) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
    	persistenceEntryManager.removeByDn(primaryKey, objectClasses);
	}

	@Override
	public <T> int remove(String primaryKey, Class<T> entryClass, Filter filter, int count) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
    	return persistenceEntryManager.remove(primaryKey, entryClass, filter, count);
	}

	@Override
    public void removeDeleteSubscriber(DeleteNotifier subscriber) {
        if (this.persistenceEntryManagers == null) {
            return;
        }

        for (PersistenceEntryManager persistenceEntryManager : persistenceEntryManagers.values()) {
    		persistenceEntryManager.removeDeleteSubscriber(subscriber);
        }
    }

	@Override
    public <T> void removeRecursivelyFromDn(String dn, String[] objectClasses) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	persistenceEntryManager.removeRecursivelyFromDn(dn, objectClasses);
    }

    //*************************************************************************
    // Internal methods which not needed in Hybrid Entry Manager
    //*************************************************************************

	@Override
	protected void persist(String dn, String[] objectClasses, List<AttributeData> attributes, Integer expiration) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

    @Override
	protected void merge(String dn, String[] objectClasses, List<AttributeDataModification> attributeDataModifications, Integer expiration) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
    protected List<AttributeData> find(String dn, String[] objectClasses, Map<String, PropertyAnnotation> propertiesAnnotationsMap, String... ldapReturnAttributes) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

    @Override
	protected <T> boolean contains(String baseDN, String[] objectClasses, Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, Filter filter, String[] ldapReturnAttributes) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

    @Override
	protected Date decodeTime(String date) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	protected String encodeTime(Date date) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	protected <T> void updateMergeChanges(String baseDn, T entry, boolean isConfigurationUpdate, Class<?> entryClass,
			Map<String, AttributeData> attributesFromLdapMap, List<AttributeDataModification> attributeDataModifications, boolean forceUpdate) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	protected Object getNativeDateAttributeValue(Date dateValue) {
        throw new UnsupportedOperationException("Method not implemented.");
	}


}
