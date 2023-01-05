/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Date;

@ApplicationScoped
public class NativePersistenceCacheProvider extends AbstractCacheProvider<PersistenceEntryManager> {

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    private String baseDn;

	private boolean deleteExpiredOnGetRequest;

	private boolean skipRemoveBeforePut;

	private boolean attemptUpdateBeforeInsert;

    @PostConstruct
    public void init() {
    }

    @Override
    public void create() {
        try {
            baseDn = cacheConfiguration.getNativePersistenceConfiguration().getBaseDn();
            deleteExpiredOnGetRequest = cacheConfiguration.getNativePersistenceConfiguration().isDeleteExpiredOnGetRequest();

            if (StringUtils.isBlank(baseDn)) {
                log.error("Failed to create NATIVE_PERSISTENCE cache provider. 'baseDn' in CacheConfiguration is not initialized. It has to be set by client application (e.g. oxAuth has to set it in ApplicationFactory.)");
                throw new RuntimeException("Failed to create NATIVE_PERSISTENCE cache provider. 'baseDn' in CacheConfiguration is not initialized. It has to be set by client application.");
            }

            String branchDn = String.format("ou=cache,%s", baseDn);
            if (entryManager.hasBranchesSupport(branchDn)) {
	            if (!entryManager.contains(branchDn, SimpleBranch.class)) {
	                SimpleBranch branch = new SimpleBranch();
	                branch.setOrganizationalUnitName("cache");
	                branch.setDn(branchDn);
	
	                entryManager.persist(branch);
	            }
            }

            baseDn = branchDn;
            cacheConfiguration.getNativePersistenceConfiguration().setBaseDn(baseDn);

            String persistenceType = entryManager.getPersistenceType(baseDn);
            // CouchbaseEntryManagerFactory.PERSISTENCE_TYPE
            skipRemoveBeforePut = "couchbase".equals(persistenceType);
            attemptUpdateBeforeInsert = "sql".equals(persistenceType);
            if (cacheConfiguration.getNativePersistenceConfiguration().isDisableAttemptUpdateBeforeInsert()) {
                attemptUpdateBeforeInsert = false;
            }

            log.info("Created NATIVE_PERSISTENCE cache provider. `baseDn`: " + baseDn);
        } catch (Exception e) {
            log.error("Failed to create NATIVE_PERSISTENCE cache provider.", e);
            throw new RuntimeException("Failed to create NATIVE_PERSISTENCE cache provider.", e);
        }
    }

	public void configure(CacheConfiguration cacheConfiguration, PersistenceEntryManager entryManager) {
		this.log = LoggerFactory.getLogger(NativePersistenceCacheProvider.class);
		this.cacheConfiguration = cacheConfiguration;
		this.entryManager = entryManager;
	}

    @Override
    public void destroy() {
    }

    @Override
    public PersistenceEntryManager getDelegate() {
        return entryManager;
    }

	@Override
	public boolean hasKey(String key) {
        try {
            key = hashKey(key);
            boolean hasKey = entryManager.contains(createDn(key), NativePersistenceCacheEntity.class);
            
//            log.trace("Contains key in cache, key: " + key + ", dn: " + createDn(key)) + ", contains: " + hasKey);
            return hasKey;
        } catch (Exception e) {
            // ignore, we call cache first which is empty and then fill it in
            // log.trace("No entry with key: " + originalKey + ", message: " + e.getMessage() + ", hashedKey: " + key);
        }

        return false;
	}

    @Override
    public Object get(String key) {
        try {
            key = hashKey(key);
            NativePersistenceCacheEntity entity = entryManager.find(NativePersistenceCacheEntity.class, createDn(key));
            if (entity != null && entity.getData() != null) {
                if (isExpired(entity.getExpirationDate()) && entity.isDeletable()) {
                    log.trace("Cache entity exists but expired, return null, expirationDate:" + entity.getExpirationDate() + ", key: " + key);
                    if (deleteExpiredOnGetRequest && !skipRemoveBeforePut) {
                    	remove(key);
                    }
                    return null;
                }
                Object o = fromString(entity.getData());
//                log.trace("Returned object from cache, key: " + key + ", dn: " + entity.getDn());
                return o;
            }
        } catch (Exception e) {
            // ignore, we call cache first which is empty and then fill it in
            // log.trace("No entry with key: " + originalKey + ", message: " + e.getMessage() + ", hashedKey: " + key);
        }
        return null;
    }

    private String createDn(String key) {
        return String.format("uuid=%s,%s", key, baseDn);
    }

    public static String hashKey(String key) {
        return DigestUtils.sha256Hex(key);
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        Date creationDate = new Date();

        expirationInSeconds = expirationInSeconds > 0 ? expirationInSeconds : cacheConfiguration.getNativePersistenceConfiguration().getDefaultPutExpiration();

        putImpl(key, object, creationDate, expirationInSeconds);
    }

	private void putImpl(String key, Object object, Date creationDate, int expirationInSeconds) {
        Calendar expirationDate = Calendar.getInstance();
		expirationDate.setTime(creationDate);
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

		
		String originalKey = key;

        key = hashKey(key);

        NativePersistenceCacheEntity entity = new NativePersistenceCacheEntity();
        entity.setTtl(expirationInSeconds);
        entity.setData(asString(object));
        entity.setId(key);
        entity.setDn(createDn(key));
        entity.setCreationDate(creationDate);
        entity.setExpirationDate(expirationDate.getTime());
        entity.setDeletable(true);

        try {
        	if (attemptUpdateBeforeInsert) {
                entryManager.merge(entity);
        	} else {
				if (!skipRemoveBeforePut) {
					silentlyRemoveEntityIfExists(entity.getDn());
				}
				entryManager.persist(entity);
        	}
        } catch (EntryPersistenceException e) {
            if (e.getCause() instanceof DuplicateEntryException) { // on duplicate, remove entry and try to persist again
                try {
                    silentlyRemoveEntityIfExists(entity.getDn());
                    entryManager.persist(entity);
                    return;
                } catch (Exception ex) {
                    log.error("Failed to retry put entry, key: " + originalKey + ", hashedKey: " + key + ", message: " + ex.getMessage(), ex);
                }
            }

			if (attemptUpdateBeforeInsert) {
				try {
					entryManager.persist(entity);
					return;
				} catch (Exception ex) {
					log.error("Failed to retry put entry, key: " + originalKey + ", hashedKey: " + key + ", message: " + ex.getMessage(), ex);
				}
			}

            log.error("Failed to put entry, key: " + originalKey + ", hashedKey: " + key + ", message: " + e.getMessage(), e);
        } catch (Exception e) {
        	log.error("Failed to put entry, key: " + originalKey + ", hashedKey: " + key + ", message: " + e.getMessage(), e); // log as trace since it is perfectly valid that entry is removed by timer for example
        }
	}

    private boolean silentlyRemoveEntityIfExists(String dn) {
        try {
            if (entryManager.find(NativePersistenceCacheEntity.class, dn) != null) {
                entryManager.remove(dn, NativePersistenceCacheEntity.class);
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private static boolean isExpired(Date expiredAt) {
        return expiredAt == null || expiredAt.before(new Date());
    }

    @Override
    public void remove(String key) {
        if (silentlyRemoveEntityIfExists(createDn(hashKey(key)))) {
            log.trace("Removed entity, key: " + key);
        }
    }

    @Override
    public void clear() {
        // TODO: Implement all specific application objects removal
    }

    private Object fromString(String s) {
        try {
            byte[] data = Base64.decodeBase64(s);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            try {
                Object o = ois.readObject();
                ois.close();
                return o;
            } finally {
                IOUtils.closeQuietly(ois);
            }
        } catch (Exception e) {
            log.error("Failed to deserizalize cache entity, data: " + s, e);
            return null;
        }
    }

    private String asString(Object o) {
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return Base64.encodeBase64String(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to serizalize cache entity to string, object: " + 0, e);
            return null;
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }

    @Override
    public void cleanup(final Date now) {
    	NativePersistenceConfiguration nativePersistenceConfiguration = cacheConfiguration.getNativePersistenceConfiguration();
		if (!entryManager.hasExpirationSupport(nativePersistenceConfiguration.getBaseDn())) {
			cleanup(now, cacheConfiguration.getNativePersistenceConfiguration().getDefaultCleanupBatchSize());
		}
    }

    public void cleanup(final Date now, int batchSize) {
        log.debug("Start NATIVE_PERSISTENCE clean up");
        try {
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("del", true),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn, now)));
            final int removedCount = entryManager.remove(baseDn, NativePersistenceCacheEntity.class, filter, batchSize);

            log.debug("End NATIVE_PERSISTENCE clean up, items removed: " + removedCount);
        } catch (Exception e) {
            log.error("Failed to perform clean up.", e);
        }

    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.NATIVE_PERSISTENCE;
    }

    /* required for tests */
    public void setEntryManager(PersistenceEntryManager entryManager) {
        this.entryManager = entryManager;
    }

    /* required for tests */
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    /* required for tests */
    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }
    
    public static void main(String[] args) {
		NativePersistenceCacheProvider cp = new NativePersistenceCacheProvider();
		Object obj = cp.fromString("rO0ABXNyAClvcmcuZ2x1dS5veGF1dGgubW9kZWwuY29tbW9uLkNsaWVudFRva2Vuc/Aib54fThHVAgACTAAIY2xpZW50SWR0ABJMamF2YS9sYW5nL1N0cmluZztMAAt0b2tlbkhhc2hlc3QAD0xqYXZhL3V0aWwvU2V0O3hwdAApMTAwMS45MGQ0MGI2OS02ZDFmLTQxMmYtOTg5ZS00MThmN2E2Y2M1MTNzcgARamF2YS51dGlsLkhhc2hTZXS6RIWVlri3NAMAAHhwdwwAAAAQP0AAAAAAAAF0AEA3M2M1NDBhYjRlNzU2ZTk2ZjQ2NzU2ODZjNzU0ZDg1ZjZiOWExYmI0ZjI1ZWY5NTZjYmRkZTQ0NjlmZTA2OGVjeA==");
		
		System.out.println(obj);
	}
}
