/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.hybrid.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.exception.KeyConversionException;
import org.gluu.persist.exception.MappingException;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.impl.BaseEntryManager;
import org.gluu.persist.key.impl.GenericKeyConverter;
import org.gluu.persist.key.impl.model.ParsedKey;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.AttributeDataModification;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.search.filter.Filter;
import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid Entry Manager
 *
 * @author Yuriy Movchan Date: 07/10/2019
 */
public class HybridEntryManager extends BaseEntryManager implements Serializable {

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
    public boolean authenticate(String bindDn, String password) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(bindDn);
    	return persistenceEntryManager.authenticate(bindDn, password);
    }

    @Override
    public boolean authenticate(String baseDN, String userName, String password) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.authenticate(baseDN, userName, password);
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
	protected boolean contains(String baseDN, Filter filter, String[] objectClasses, String[] ldapReturnAttributes) {
        throw new UnsupportedOperationException("Method not implemented.");
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
	protected Date decodeTime(String date) {
        throw new UnsupportedOperationException("Method not implemented.");
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
	protected String encodeTime(Date date) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
    public String encodeTime(String baseDN, Date date) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(baseDN);
    	return persistenceEntryManager.encodeTime(baseDN, date);
    }

	@Override
    public String[] exportEntry(String dn) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	return persistenceEntryManager.exportEntry(dn);
    }

	@Override
	public <T> T find(Class<T> entryClass, Object primaryKey) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
    	return persistenceEntryManager.find(entryClass, primaryKey);
	}

	@Override
	public <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(primaryKey);
    	return persistenceEntryManager.find(entryClass, primaryKey, ldapReturnAttributes);
	}

	@Override
	protected List<AttributeData> find(String dn, String... attributes) {
        throw new UnsupportedOperationException("Method not implemented.");
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

    private PersistenceEntryManager getBucketMappingByKey(String key) {
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
		return getBucketMappingByKey(parsedKey.getKey());
	}

	public HybridPersistenceOperationService getOperationService() {
        return operationService;
    }

	@Override
	public boolean hasBranchesSupport(String dn) {
		PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	return persistenceEntryManager.hasBranchesSupport(dn);
	}

    protected void init() {
        String defaultPersistenceType = mappingProperties.getProperty("default", null);
        if (StringHelper.isEmpty(defaultPersistenceType) || (persistenceEntryManagers.get(defaultPersistenceType) == null)) {
            throw new ConfigurationException("Default persistence type is not defined!");
        }
        this.defaultPersistenceEntryManager = persistenceEntryManagers.get(defaultPersistenceType);

        this.baseNameToEntryManagerMapping = new HashMap<String, PersistenceEntryManager>();
        for (Entry<String, PersistenceEntryManager> persistenceTypeEntry : persistenceEntryManagers.entrySet()) {
        	String mapping = mappingProperties.getProperty(persistenceTypeEntry.getKey() + ".mapping", "");
            String[] baseNames = StringHelper.split(mapping, ",");
            for (String baseName : baseNames) {
            	baseNameToEntryManagerMapping.put(baseName, persistenceTypeEntry.getValue());
            }
        }
    }

    @Override
	protected void merge(String dn, List<AttributeDataModification> attributeDataModifications) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

    //*************************************************************************
    // Internal methods which not needed in Hybrid Entry Manager
    //*************************************************************************

    @Override
    public <T> T merge(T entry) {
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
	protected void persist(String dn, List<AttributeData> attributes) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
    public void remove(Object entry) {
        Class<?> entryClass = entry.getClass();
        Object dnValue = getDNValue(entry, entryClass);

    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dnValue);
    	persistenceEntryManager.remove(entry);
    }

	@Override
	protected void remove(String dn) {
        throw new UnsupportedOperationException("Method not implemented.");
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
    public void removeRecursively(String dn) {
    	PersistenceEntryManager persistenceEntryManager = getEntryManagerForDn(dn);
    	persistenceEntryManager.removeRecursively(dn);
    }

	@Override
	protected <T> void updateMergeChanges(String baseDn, T entry, boolean isSchemaUpdate, Class<?> entryClass,
			Map<String, AttributeData> attributesFromLdapMap, List<AttributeDataModification> attributeDataModifications) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

}
