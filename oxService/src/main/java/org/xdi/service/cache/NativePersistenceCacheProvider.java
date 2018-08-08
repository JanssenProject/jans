package org.xdi.service.cache;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.StaticUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.SimpleBranch;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class NativePersistenceCacheProvider extends AbstractCacheProvider<LdapEntryManager> {

    private final static Logger log = LoggerFactory.getLogger(NativePersistenceCacheProvider.class);

    @Inject
    CacheConfiguration cacheConfiguration;
    @Inject
    LdapEntryManager ldapEntryManager;

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
    public LdapEntryManager getDelegate() {
        return ldapEntryManager;
    }

    @Override
    public Object get(String region, String key) {
        try {
            NativePersistenceCacheEntity entity = ldapEntryManager.find(NativePersistenceCacheEntity.class, createDn(key));
            if (entity != null && entity.getData() != null) {
                if (isExpired(entity.getExpirationDate())) {
                    log.trace("Cache entity exists but expired, return null, expirationDate:" + entity.getExpirationDate() + ", key: " + key);
                    remove("", key);
                    return null;
                }
                return fromString(entity.getData());
            }
        } catch (Exception e) {
            log.trace("Didn't find entry by key: " + key + ", message: " + e.getMessage());
        }
        return null;
    }

    private String createDn(String key) {
        return String.format("uniqueIdentifier=%s,%s", key, baseDn);
    }

    @Override
    public void put(String expirationInSeconds, String key, Object object) {
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

        ldapEntryManager.persist(entity);
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
        ldapEntryManager.removeWithSubtree(createDn(key));
    }

    @Override
    public void clear() {
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

    public void cleanup(final Date now, int batchSize) {
        log.debug("Start NATIVE_PERSISTENCE clean up");

        BatchOperation<NativePersistenceCacheEntity> clientBatchService = new BatchOperation<NativePersistenceCacheEntity>(ldapEntryManager) {
            private Filter getFilter() {
                try {
                    return Filter.create(String.format("(oxAuthExpiration<=%s)", StaticUtils.encodeGeneralizedTime(now)));
                }catch (LDAPException e) {
                    log.trace(e.getMessage(), e);
                    return Filter.createPresenceFilter("oxAuthExpiration");
                }
            }

            @Override
            protected List<NativePersistenceCacheEntity> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(baseDn, NativePersistenceCacheEntity.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<NativePersistenceCacheEntity> entries) {
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
        clientBatchService.iterateAllByChunks(batchSize);

        log.debug("End NATIVE_PERSISTENCE clean up");
    }
}
