package io.jans.cedarling.binding.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.io.File;
import java.util.*;

import uniffi.cedarling_uniffi.*;

import static org.testng.Assert.*;

/*
 * To check the Policy Store used in UnsignedAuthzTest.java test-cases, upload the
 * `./src/test/resources/config/unsigned/policy-store/policy-store.cjar` in Agama-Lab Policy designer.
 *
 */
public class UnsignedAuthzTest {

    private CedarlingAdapter adapter;

    private String action;
    private JSONObject resource;
    private JSONObject context;
    private String principalsString;
    ObjectMapper mapper = new ObjectMapper();

    private static final String BASE_PATH = "./src/test/resources/config/unsigned/";

    private static final String BOOTSTRAP_PATH = BASE_PATH + "bootstrap.json";
    private static final String PRINCIPALS_PATH = BASE_PATH + "principals.json";
    private static final String ACTION_PATH = BASE_PATH + "action.txt";
    private static final String RESOURCE_PATH = BASE_PATH + "resource.json";
    private static final String CONTEXT_PATH = BASE_PATH + "context.json";

    @BeforeMethod
    public void setUp() throws Exception {
        adapter = new CedarlingAdapter();
        String bootstrapJsonString = readFile(BOOTSTRAP_PATH);

        adapter.loadFromJson(updatePolicyStorePathInBootstrap(bootstrapJsonString));
        assertNotNull(adapter.getCedarling(), "Cedarling should be initialized");

        action = readFile(ACTION_PATH);
        resource = new JSONObject(readFile(RESOURCE_PATH));
        context = new JSONObject(readFile(CONTEXT_PATH));
        principalsString = readFile(PRINCIPALS_PATH);
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private String updatePolicyStorePathInBootstrap(String bootstrapJsonString) throws JsonProcessingException {
        JsonNode bootstrapJson = mapper.readTree(bootstrapJsonString);

        File resourcesDirectory = new File("src/test/resources");
        String absolutePath = resourcesDirectory.getAbsolutePath() + "/config/unsigned/policy-store";

        ((ObjectNode) bootstrapJson).put("CEDARLING_POLICY_STORE_LOCAL_FN",
                absolutePath);
        return bootstrapJson.toString();
    }
    private String readFile(String path) throws Exception {
        return AppUtils.readFile(path);
    }

    private EntityData createPrincipalsList() throws EntityException {
        JSONObject principalsJO = new JSONObject(principalsString);
        return EntityData.Companion.fromJson(principalsJO.toString());
    }

    // ---------------------------
    // Authorization Tests
    // ---------------------------

    @Test
    public void testAuthorizeWithPrincipalsList() throws Exception {
        AuthorizeResult result = adapter.authorizeUnsignedEntity(
                createPrincipalsList(), action, resource, context);

        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getResponse(), "Response should not be null");
        assertTrue(result.getDecision(), "Authorization should ALLOW");
    }

    @Test
    public void testAuthorizeWithJsonString() throws Exception {
        AuthorizeResult result = adapter.authorizeUnsigned(
                principalsString, action, resource, context);

        assertNotNull(result);
        assertTrue(result.getDecision(), "Authorization should ALLOW");
    }

    @Test(expectedExceptions = EntityException.class)
    public void testAuthorizeDeniedInvalidPrincipals() throws AuthorizeException, EntityException {
        String invalidPrincipals = "{}";
        adapter.authorizeUnsigned(
                invalidPrincipals, action, resource, context);
    }

    // ---------------------------
    // Logging Tests
    // ---------------------------

    @Test(dependsOnMethods = "testAuthorizeWithPrincipalsList")
    public void testGetLogIds() {
        List<String> logEntries = adapter.getLogIds();
        assertFalse(logEntries.isEmpty(), "Log IDs should not be empty");
    }

    @Test(dependsOnMethods = "testAuthorizeWithPrincipalsList")
    public void testGetLogById() throws Exception {
        List<String> logEntries = adapter.getLogIds();

        String log = adapter.getLogById(logEntries.get(0));
        assertNotNull(log, "Log entry should not be null");

    }

    @Test(dependsOnMethods = "testAuthorizeWithPrincipalsList")
    public void testGetLogsByTag() throws LogException {
        List<String> logs = adapter.getLogsByTag("System");
        assertFalse(logs.isEmpty(), "Logs by tag should not be empty");
    }

    @Test(dependsOnMethods = "testAuthorizeWithPrincipalsList")
    public void testPopLogs() throws LogException {
        List<String> logs = adapter.popLogs();
        assertFalse(logs.isEmpty(), "Popped logs should not be empty");
    }

    // ---------------------------
    // Data Context Tests
    // ---------------------------

    @Test
    public void testPushAndGetData() throws DataException {
        adapter.pushDataCtx("key1", new JSONObject().put("data", "value1"));

        JSONObject result = (JSONObject) adapter.getDataCtx("key1");
        assertEquals(result.getString("data"), "value1");

        adapter.pushDataCtx("key2", new JSONObject().put("nested", "data"), 60L);
        JSONObject result2 = (JSONObject) adapter.getDataCtx("key2");
        assertEquals(result2.getString("nested"), "data");

        adapter.pushDataCtx("key3", "[1,2,3]", null);
        assertTrue(adapter.getDataCtx("key3") instanceof JSONArray);
    }

    @Test
    public void testTTLExpiry() throws Exception {
        adapter.pushDataCtx("ttlKey", new JSONObject().put("data", "temp"), 1L);

        Thread.sleep(1500);

        assertNull(adapter.getDataCtx("ttlKey"), "Data should expire after TTL");
    }

    @Test
    public void testRemoveData() throws DataException {
        adapter.pushDataCtx("key", new JSONObject());

        assertTrue(adapter.removeDataCtx("key"));
        assertNull(adapter.getDataCtx("key"));

        assertFalse(adapter.removeDataCtx("nonexistent"));
    }

    @Test
    public void testClearData() throws DataException {
        adapter.pushDataCtx("k1", new JSONObject());
        adapter.pushDataCtx("k2", new JSONObject());

        adapter.clearDataCtx();

        assertNull(adapter.getDataCtx("k1"));
        assertNull(adapter.getDataCtx("k2"));
    }

    @Test
    public void testListData() throws DataException {
        adapter.pushDataCtx("k1", new JSONObject());
        adapter.pushDataCtx("k2", new JSONObject());

        List<DataEntry> entries = adapter.listDataCtx();

        assertEquals(entries.size(), 2);

        Set<String> keys = new HashSet<>();
        for (DataEntry entry : entries) {
            keys.add(entry.getKey());
        }

        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
    }

    @Test
    public void testGetStats() throws DataException {
        assertEquals(adapter.listDataCtx().size(), 0);

        adapter.pushDataCtx("k1", new JSONObject());
        adapter.pushDataCtx("k2", new JSONObject());

        DataStoreStats stats = adapter.getStatsCtx();

        assertNotNull(stats);
        assertEquals(adapter.listDataCtx().size(), 2);
        assertTrue(stats.getMetricsEnabled());
    }
}