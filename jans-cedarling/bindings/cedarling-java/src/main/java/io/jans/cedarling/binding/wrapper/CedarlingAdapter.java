package io.jans.cedarling.binding.wrapper;

import uniffi.cedarling_uniffi.*;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * High-level wrapper around the Cedarling UniFFI binding.
 *
 * <p>This adapter hides the UniFFI-generated types from application code by
 * providing convenience methods that accept standard Java types ({@code Map},
 * {@code String}, {@code JSONObject}).  The lower-level overloads that accept
 * {@link EntityData} and {@link TokenInput} directly are still available for
 * advanced use cases.</p>
 *
 * <h3>Migration from v16.0</h3>
 * <p>The legacy {@code authorize(Map&lt;String,String&gt; tokens, ...)} method
 * has been replaced by two dedicated methods:</p>
 * <ul>
 *   <li>{@link #authorizeMultiIssuer(Map, String, JSONObject, JSONObject)} –
 *       drop-in replacement that takes a token map, validates JWTs, and evaluates
 *       policies.</li>
 *   <li>{@link #authorizeUnsigned(String, String, JSONObject, JSONObject)} –
 *       for pre-validated / unsigned entity data.</li>
 * </ul>
 */
public class CedarlingAdapter implements AutoCloseable {

    Cedarling cedarling;

    public CedarlingAdapter() {}

    public void loadFromJson(String bootstrapConfigJson) throws CedarlingException {
        this.cedarling = Cedarling.Companion.loadFromJson(bootstrapConfigJson);
    }

    public void loadFromFile(String path) throws CedarlingException {
        this.cedarling = Cedarling.Companion.loadFromFile(path);
    }

    // ── authorize_multi_issuer ──────────────────────────────────────────

    /**
     * Authorize using JWT tokens from multiple issuers.
     *
     * <p>This is the recommended replacement for the removed
     * {@code authorize(Map&lt;String,String&gt;, ...)} method.  Each map entry
     * is a token mapping name (e.g. {@code "Jans::Access_Token"}) to the raw
     * JWT string.</p>
     *
     * @param tokens  mapping name → JWT string (must not be null; no null keys or values)
     * @param action  Cedar action (e.g. {@code "Jans::Action::\"Read\""})
     * @param resource resource as JSONObject (must not be null)
     * @param context  context as JSONObject (may be null; sent as empty JSON object to the engine)
     * @return authorization result
     */
    public MultiIssuerAuthorizeResult authorizeMultiIssuer(
            Map<String, String> tokens,
            String action,
            JSONObject resource,
            JSONObject context) throws AuthorizeException, EntityException {

        if (tokens == null) {
            throw new IllegalArgumentException("tokens must not be null");
        }
        List<TokenInput> tokenInputs = new ArrayList<>();
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("tokens map must not contain a null key");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "tokens map must not contain a null value for key: " + entry.getKey());
            }
            tokenInputs.add(new TokenInput(entry.getKey(), entry.getValue()));
        }
        return authorizeMultiIssuer(tokenInputs, action, resource, context);
    }

    /**
     * Authorize using pre-built {@link TokenInput} objects.
     *
     * @param resource resource as JSONObject (must not be null)
     */
    public MultiIssuerAuthorizeResult authorizeMultiIssuer(
            List<TokenInput> tokens,
            String action,
            JSONObject resource,
            JSONObject context) throws AuthorizeException, EntityException {

        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        EntityData resourceObj = EntityData.Companion.fromJson(resource.toString());
        String contextStr = context != null ? context.toString() : "{}";
        return cedarling.authorizeMultiIssuer(tokens, action, resourceObj, contextStr);
    }

    // ── authorize_unsigned ──────────────────────────────────────────────

    /**
     * Authorize with a single principal provided as a JSON string.
     *
     * <p>This is the simplest way to call {@code authorizeUnsigned} without
     * importing any UniFFI types.  The JSON string is converted to an
     * {@link EntityData} internally.</p>
     *
     * @param principalJson single principal as a JSON string (must not be null)
     * @param action  Cedar action
     * @param resource resource as JSONObject (must not be null)
     * @param context  context as JSONObject (may be null; sent as empty JSON object to the engine)
     * @return authorization result
     */
    public AuthorizeResult authorizeUnsigned(
            String principalJson,
            String action,
            JSONObject resource,
            JSONObject context) throws AuthorizeException, EntityException {

        if (principalJson == null) {
            throw new IllegalArgumentException("principalJson must not be null");
        }
        EntityData principal = EntityData.Companion.fromJson(principalJson);
        return authorizeUnsigned(List.of(principal), action, resource, context);
    }

    /**
     * Authorize with multiple principals provided as JSON strings.
     *
     * <p>Use this when you have more than one principal and want to avoid
     * importing UniFFI types.  Each JSON string is converted to an
     * {@link EntityData} internally.</p>
     *
     * @param principalsJson principal JSON strings (must not be null; no null elements)
     * @param action  Cedar action
     * @param resource resource as JSONObject (must not be null)
     * @param context  context as JSONObject (may be null; sent as empty JSON object to the engine)
     * @return authorization result
     */
    public AuthorizeResult authorizeUnsignedFromJson(
            List<String> principalsJson,
            String action,
            JSONObject resource,
            JSONObject context) throws AuthorizeException, EntityException {

        if (principalsJson == null) {
            throw new IllegalArgumentException("principalsJson must not be null");
        }
        List<EntityData> principals = new ArrayList<>();
        for (String json : principalsJson) {
            if (json == null) {
                throw new IllegalArgumentException("principalsJson must not contain null elements");
            }
            principals.add(EntityData.Companion.fromJson(json));
        }
        return authorizeUnsigned(principals, action, resource, context);
    }

    /**
     * Authorize with pre-built {@link EntityData} principals.
     *
     * <p>Use this overload when you already have {@link EntityData} objects
     * (e.g. from advanced integration code). A null {@code context} is sent as an
     * empty JSON object to the engine.</p>
     *
     * @param resource resource as JSONObject (must not be null)
     */
    public AuthorizeResult authorizeUnsigned(
            List<EntityData> principals,
            String action,
            JSONObject resource,
            JSONObject context) throws AuthorizeException, EntityException {

        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        EntityData resourceObj = EntityData.Companion.fromJson(resource.toString());
        String contextStr = context != null ? context.toString() : "{}";
        return cedarling.authorizeUnsigned(principals, action, resourceObj, contextStr);
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
        if (key == null || key.isEmpty()) {
            throw new DataException.InvalidKey();
        }
        if (value == null) {
            throw new DataException.SerializationException("value cannot be null");
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
        if (key == null || key.isEmpty()) {
            throw new DataException.InvalidKey();
        }
        if (value == null) {
            throw new DataException.SerializationException("value cannot be null");
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
            throw new DataException.SerializationException("Failed to parse JSON result: " + e.getMessage());
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

    public boolean isTrustedIssuerLoadedByName(String issuerId) {
        return cedarling.isTrustedIssuerLoadedByName(issuerId);
    }

    public boolean isTrustedIssuerLoadedByIss(String issClaim) {
        return cedarling.isTrustedIssuerLoadedByIss(issClaim);
    }

    public long totalIssuers() {
        return cedarling.totalIssuers();
    }

    public long loadedTrustedIssuersCount() {
        return cedarling.loadedTrustedIssuersCount();
    }

    public List<String> loadedTrustedIssuerIds() {
        return cedarling.loadedTrustedIssuerIds();
    }

    public List<String> failedTrustedIssuerIds() {
        return cedarling.failedTrustedIssuerIds();
    }
}
