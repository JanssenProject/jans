package io.jans.cedarling.binding.wrapper;

import uniffi.cedarling_uniffi.*;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;

public class CedarlingAdapter implements AutoCloseable {

    Cedarling cedarling;

    public CedarlingAdapter() {}

    public void loadFromJson(String bootstrapConfigJson) throws CedarlingException {
        this.cedarling = Cedarling.Companion.loadFromJson(bootstrapConfigJson);
    }

    public void loadFromFile(String path) throws CedarlingException {
        this.cedarling = Cedarling.Companion.loadFromFile(path);
    }

    public AuthorizeResult authorize(Map<String, String> tokens, String action, JSONObject resource, JSONObject context)
            throws AuthorizeException, EntityException {
        // Build EntityData from resource JSON
        EntityData resourceObj = EntityData.Companion.fromJson(resource.toString());

        return this.cedarling.authorize(tokens, action, resourceObj, context.toString());
    }

    public AuthorizeResult authorizeUnsigned(List<EntityData> principals, String action, JSONObject resource, JSONObject context)
            throws AuthorizeException, EntityException {
        // Build EntityData from resource JSON
        EntityData resourceObj = EntityData.Companion.fromJson(resource.toString());
        return cedarling.authorizeUnsigned(principals, action, resourceObj, context.toString());
    }

    public MultiIssuerAuthorizeResult authorizeMultiIssuer(List<TokenInput> tokens, String action, JSONObject resource, JSONObject context)
            throws AuthorizeException, EntityException {
        // Build EntityData from resource JSON
        EntityData resourceObj = EntityData.Companion.fromJson(resource.toString());
        String contextStr = context != null ? context.toString() : null;
        return cedarling.authorizeMultiIssuer(tokens, action, resourceObj, contextStr);
    }

    public String getLogById(String id) throws LogException {
        return cedarling.getLogById(id);
    }

    public List<String> getLogIds() {
        return cedarling.getLogIds();
    }

    public List<String> getLogsByRequestId(String requestId) throws LogException {
        return cedarling.getLogsByRequestId(requestId);
    }

    public List<String> getLogsByRequestIdAndTag(String requestId, String tag) throws LogException {
        return cedarling.getLogsByRequestIdAndTag(requestId, tag);
    }

    public List<String> getLogsByTag(String tag) throws LogException {
        return cedarling.getLogsByTag(tag);
    }

    public List<String> popLogs() throws LogException {
        return cedarling.popLogs();
    }

    /**
     * Push a value into the data store with an optional TTL.
     * If the key already exists, the value will be replaced.
     * If TTL is not provided, the default TTL from configuration is used.
     *
     * @param key The key for the data entry
     * @param value The value to store (as JSONObject)
     * @param ttlSecs Optional TTL in seconds (null uses default from config)
     * @throws DataException If the operation fails
     */
    public void pushDataCtx(String key, JSONObject value, Long ttlSecs) throws DataException {
        if (key == null) {
            throw new DataException.DataOperationFailed("key cannot be null");
        }
        if (value == null) {
            throw new DataException.DataOperationFailed("value cannot be null");
        }
        cedarling.pushDataCtx(key, value.toString(), ttlSecs);
    }

    /**
     * Push a value into the data store with an optional TTL.
     * If the key already exists, the value will be replaced.
     * If TTL is not provided, the default TTL from configuration is used.
     *
     * @param key The key for the data entry
     * @param value The value to store (as JSON string)
     * @param ttlSecs Optional TTL in seconds (null uses default from config)
     * @throws DataException If the operation fails
     */
    public void pushDataCtx(String key, String value, Long ttlSecs) throws DataException {
        if (key == null) {
            throw new DataException.DataOperationFailed("key cannot be null");
        }
        if (value == null) {
            throw new DataException.DataOperationFailed("value cannot be null");
        }
        cedarling.pushDataCtx(key, value, ttlSecs);
    }

    /**
     * Push a value into the data store without TTL (uses default from config).
     *
     * @param key The key for the data entry
     * @param value The value to store (as JSONObject)
     * @throws DataException If the operation fails
     */
    public void pushDataCtx(String key, JSONObject value) throws DataException {
        pushDataCtx(key, value, null);
    }

    /**
     * Push a value into the data store without TTL (uses default from config).
     *
     * @param key The key for the data entry
     * @param value The value to store (as JSON string)
     * @throws DataException If the operation fails
     */
    public void pushDataCtx(String key, String value) throws DataException {
        pushDataCtx(key, value, null);
    }

    /**
     * Get a value from the data store by key.
     * Returns null if the key doesn't exist or the entry has expired.
     *
     * @param key The key to retrieve
     * @return The value as an Object (JSONObject, JSONArray, String, Number, Boolean, or null), or null if not found
     * @throws DataException If the operation fails
     */
    public Object getDataCtx(String key) throws DataException {
        String result = cedarling.getDataCtx(key);
        if (result == null) {
            return null;
        }
        try {
            org.json.JSONTokener tokener = new org.json.JSONTokener(result);
            Object value = tokener.nextValue();
            if (value == org.json.JSONObject.NULL) {
                return null;
            }
            return value;
        } catch (org.json.JSONException e) {
            throw new DataException.DataOperationFailed("Failed to parse JSON result: " + e.getMessage(), e);
        }
    }

    /**
     * Get a data entry with full metadata by key.
     * Returns null if the key doesn't exist or the entry has expired.
     *
     * @param key The key to retrieve
     * @return A DataEntry object with metadata, or null if not found
     * @throws DataException If the operation fails
     */
    public DataEntry getDataEntryCtx(String key) throws DataException {
        return cedarling.getDataEntryCtx(key);
    }

    /**
     * Remove a value from the data store by key.
     *
     * @param key The key to remove
     * @return True if the key existed and was removed, False otherwise
     * @throws DataException If the operation fails
     */
    public boolean removeDataCtx(String key) throws DataException {
        return cedarling.removeDataCtx(key);
    }

    /**
     * Clear all entries from the data store.
     *
     * @throws DataException If the operation fails
     */
    public void clearDataCtx() throws DataException {
        cedarling.clearDataCtx();
    }

    /**
     * List all entries with their metadata.
     *
     * @return A list of DataEntry objects
     * @throws DataException If the operation fails
     */
    public List<DataEntry> listDataCtx() throws DataException {
        return cedarling.listDataCtx();
    }

    /**
     * Get statistics about the data store.
     *
     * @return A DataStoreStats object
     * @throws DataException If the operation fails
     */
    public DataStoreStats getStatsCtx() throws DataException {
        return cedarling.getStatsCtx();
    }

    @Override
    public void close() {
        cedarling.close();
    }

    public Cedarling getCedarling() {
        return cedarling;
    }
}
