package io.jans.kc.spi.auth.oidc.impl;

import java.util.Map;

import org.jboss.logging.Logger;

import io.jans.kc.spi.auth.oidc.OIDCMetaCache;

import java.util.HashMap;

public class HashBasedOIDCMetaCache implements OIDCMetaCache{
    
    private static final Logger log  = Logger.getLogger(HashBasedOIDCMetaCache.class);
    private static final long DEFAULT_CACHE_TTL = 20*60; // 20 seconds 

    private long cacheEntryTtl;

    private Map<String,Map<String,CacheEntry>> cacheEntries;
    
    public HashBasedOIDCMetaCache() {
        this(DEFAULT_CACHE_TTL);
    }

    public HashBasedOIDCMetaCache(long cacheEntryTtl) {

        this.cacheEntryTtl = cacheEntryTtl;
        if(this.cacheEntryTtl == 0) {
            this.cacheEntryTtl = DEFAULT_CACHE_TTL;
        }
        this.cacheEntryTtl = this.cacheEntryTtl * 1000; // convert to milliseconds
        this.cacheEntries = new HashMap<String,Map<String,CacheEntry>>();
    }

    @Override
    public void put(String issuer, String key, Object value) {

        synchronized(cacheEntries) {
            createIfNotExistIssuerCacheEntry(issuer);
            addIssuerCacheEntry(issuer,key,value);
            performHouseCleaning();
        }
     }

    @Override
    public Object get(String issuer, String key) {

        synchronized(cacheEntries) {
            if(issuerCacheEntryIsMissing(issuer)) {
                performHouseCleaning();
                return null;
            }

            Object ret = getIssuerCacheEntryValue(issuer, key);
            performHouseCleaning();
            return ret;
        }
    }

    private boolean issuerCacheEntryIsMissing(String issuer) {

        return cacheEntries.get(issuer) == null;
    }

    private Object getIssuerCacheEntryValue(String issuer, String key) {
        
        Map<String,CacheEntry> issuer_cache = cacheEntries.get(issuer);
        return issuer_cache.get(key).getValue();
    }

    private void createIfNotExistIssuerCacheEntry(String issuer) {

        if(!cacheEntries.containsKey(issuer)) {
            cacheEntries.put(issuer,new HashMap<String,CacheEntry>());
        }
    }

    private void addIssuerCacheEntry(String issuer,String key, Object value) {

        Map<String,CacheEntry> issuerCache = cacheEntries.get(issuer);
        for(String existingkey : issuerCache.keySet()) {
            if(existingkey.equalsIgnoreCase(key)) {
                //update cache entry
                CacheEntry cache_entry = issuerCache.get(existingkey);
                cache_entry.updateValue(value);
                return;
            }
        }

        issuerCache.put(key,new CacheEntry(cacheEntryTtl, value));
    }

    private void performHouseCleaning() {

        for(String issuer: cacheEntries.keySet()) {
            Map<String,CacheEntry> issuer_cache = cacheEntries.get(issuer);
            for(String key :issuer_cache.keySet()) {
                CacheEntry cache_entry = issuer_cache.get(key);
                if(cache_entry.isExpired()) {
                    issuer_cache.remove(key);
                }
            }
        }
    }

    private class CacheEntry {

        private long updateTime;
        private long ttl;
        private Object value;

        public CacheEntry(long ttl, Object value) {

            this.updateTime = System.currentTimeMillis();
            this.ttl = ttl;
            this.value = value;
        }

        public Object getValue() {

            return this.value;
        }

        public boolean isExpired()  {

            return (System.currentTimeMillis() - this.updateTime) > (this.ttl * 1000) ;
        }

        public void updateValue(Object value) {

            this.value = value;
            this.updateTime = System.currentTimeMillis();
        }

        public void refresh() {

            this.updateTime = System.currentTimeMillis();
        }
    }
}
