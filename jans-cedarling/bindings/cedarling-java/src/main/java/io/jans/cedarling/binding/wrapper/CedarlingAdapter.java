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

    @Override
    public void close() {
        cedarling.close();
    }

    public Cedarling getCedarling() {
        return cedarling;
    }
}
