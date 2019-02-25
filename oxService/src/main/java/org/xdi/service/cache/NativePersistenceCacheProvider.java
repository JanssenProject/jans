package org.xdi.service.cache;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

@ApplicationScoped
public class NativePersistenceCacheProvider extends AbstractCacheProvider<PersistenceEntryManager> {

    private final static Logger log = LoggerFactory.getLogger(NativePersistenceCacheProvider.class);

    @Inject
    private CacheConfiguration cacheConfiguration;

    @Inject
    PersistenceEntryManager ldapEntryManager;

    private String baseDn;

    @Override
    public void create() {
        try {
            baseDn = cacheConfiguration.getNativePersistenceConfiguration().getBaseDn();

            if (StringUtils.isBlank(baseDn)) {
                log.error("Failed to create NATIVE_PERSISTENCE cache provider. 'baseDn' in LdapCacheConfiguration is not initialized. It has to be set by client application (e.g. oxAuth has to set it in ApplicationFactory.)");
                throw new RuntimeException("Failed to create NATIVE_PERSISTENCE cache provider. 'baseDn' in LdapCacheConfiguration is not initialized. It has to be set by client application.");
            }

            String branchDn = String.format("ou=cache,%s", baseDn);
            if (!ldapEntryManager.contains(SimpleBranch.class, branchDn)) {
                SimpleBranch branch = new SimpleBranch();
                branch.setOrganizationalUnitName("cache");
                branch.setDn(branchDn);

                ldapEntryManager.persist(branch);
            }

            baseDn = branchDn;

            log.info("Created NATIVE_PERSISTENCE cache provider. `baseDn`: " + baseDn);
        } catch (Exception e) {
            log.error("Failed to create NATIVE_PERSISTENCE cache provider.", e);
            throw new RuntimeException("Failed to create NATIVE_PERSISTENCE cache provider.", e);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public PersistenceEntryManager getDelegate() {
        return ldapEntryManager;
    }

    @Override
    public Object get(String region, String key) {
        String originalKey = key;
        try {
            key = hashKey(key);
            NativePersistenceCacheEntity entity = ldapEntryManager.find(NativePersistenceCacheEntity.class, createDn(key));
            if (entity != null && entity.getData() != null) {
                if (isExpired(entity.getExpirationDate()) && entity.isDeletable()) {
                    log.trace("Cache entity exists but expired, return null, expirationDate:" + entity.getExpirationDate() + ", key: " + key);
                    remove("", key);
                    return null;
                }
                Object o = fromString(entity.getData());
                //log.trace("Returned object from cache, key: " + originalKey + ", dn: " + entity.getDn());
                return o;
            }
        } catch (Exception e) {
            // ignore, we call cache first which is empty and then fill it in
            // log.trace("No entry with key: " + originalKey + ", message: " + e.getMessage() + ", hashedKey: " + key);
        }
        return null;
    }

    private String createDn(String key) {
        return String.format("uniqueIdentifier=%s,%s", key, baseDn);
    }

    private static String hashKey(String key) {
        return DigestUtils.sha256Hex(key);
    }

    @Override
    public void put(String expirationInSeconds, String key, Object object) {
        String originalKey = key;

        try {
            key = hashKey(key);
            Date creationDate = new Date();

            Calendar expirationDate = Calendar.getInstance();
            expirationDate.setTime(creationDate);
            expirationDate.add(Calendar.SECOND, putExpiration(expirationInSeconds));

            NativePersistenceCacheEntity entity = new NativePersistenceCacheEntity();
            entity.setData(asString(object));
            entity.setId(key);
            entity.setDn(createDn(key));
            entity.setCreationDate(creationDate);
            entity.setExpirationDate(expirationDate.getTime());
            entity.setDeletable(true);

            silentlyRemoveEntityIfExists(entity.getDn());
            ldapEntryManager.persist(entity);
        } catch (Exception e) {
            log.trace("Failed to put entry, key: " + originalKey + ", hashedKey: " + key + ", message: " + e.getMessage(), e); // log as trace since it is perfectly valid that entry is removed by timer for example
        }
    }

    private boolean silentlyRemoveEntityIfExists(String dn) {
        try {
            if (ldapEntryManager.find(NativePersistenceCacheEntity.class, dn) != null) {
                ldapEntryManager.removeRecursively(dn);
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

    private int putExpiration(String expirationInSeconds) {
        try {
            return Integer.parseInt(expirationInSeconds);
        } catch (Exception e) {
            return cacheConfiguration.getNativePersistenceConfiguration().getDefaultPutExpiration();
        }
    }

    @Override
    public void remove(String region, String key) {
        if (silentlyRemoveEntityIfExists(createDn(hashKey(key)))) {
            log.trace("Removed entity, key: " + key);
        }
    }

    @Override
    public void clear() {
        // TODO: Implement all specific application objects removal
    }

    private static Object fromString(String s) {
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
            log.error("Failed to deserizalize cache entity, data: " + s);
            return null;
        }
    }

    private static String asString(Object o) {
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return Base64.encodeBase64String(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to serizalize cache entity to string, object: " + 0);
            return null;
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }

    @Override
    public void cleanup(final Date now) {
        cleanup(now, cacheConfiguration.getNativePersistenceConfiguration().getDefaultCleanupBatchSize());
    }

    public void cleanup(final Date now, int batchSize) {
        log.debug("Start NATIVE_PERSISTENCE clean up");
        try {
            BatchOperation<NativePersistenceCacheEntity> batchOperation = new ProcessBatchOperation<NativePersistenceCacheEntity>() {
                @Override
                public void performAction(List<NativePersistenceCacheEntity> entries) {
                    for (NativePersistenceCacheEntity entity : entries) {
                        try {
                            GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                            GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                            expirationDate.setTime(entity.getExpirationDate());
                            if (expirationDate.before(now)) {
                                log.debug("Removing NATIVE_PERSISTENCE entity: {}, Expiration date: {}", entity.getDn(), entity.getExpirationDate());
                                ldapEntryManager.remove(entity);
                            }
                        } catch (Exception e) {
                            log.error("Failed to remove entry", e);
                        }
                    }
                }
            };
            Filter filter = Filter.createLessOrEqualFilter("oxAuthExpiration", ldapEntryManager.encodeTime(now));
            ldapEntryManager.findEntries(baseDn, NativePersistenceCacheEntity.class, filter, SearchScope.SUB, null, batchOperation, 0, 0, batchSize);
            log.debug("End NATIVE_PERSISTENCE clean up");
        } catch (Exception e) {
            log.error("Failed to perform clean up.", e);
        }

    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.NATIVE_PERSISTENCE;
    }

}
