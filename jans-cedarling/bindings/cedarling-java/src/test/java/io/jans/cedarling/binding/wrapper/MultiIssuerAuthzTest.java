package io.jans.cedarling.binding.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.cedarling.binding.wrapper.jwt.JWTCreator;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.io.File;
import java.util.*;

import uniffi.cedarling_uniffi.*;

import static org.testng.Assert.*;
/*
 * To check the Policy Store used in MultiIssuerAuthzTest.java test-cases, upload the
 * `./src/test/resources/config/multiIssuer/policy-store/policy-store.cjar` in Agama-Lab Policy designer.
 *
 */
public class MultiIssuerAuthzTest {

    private CedarlingAdapter adapter;
    private JWTCreator jwtCreator;

    private String action;
    private JSONObject resource;
    ObjectMapper mapper = new ObjectMapper();
    // Constants
    private static final String JWT_SECRET = "-very-strong-shared-secret-of-at-least-32-bytes!";

    private static final String ACCESS_TOKEN_KEY = "Jans::Access_token";
    private static final String ID_TOKEN_KEY = "Jans::id_token";
    private static final String USERINFO_TOKEN_KEY = "Jans::Userinfo_token";

    private static final String BASE_PATH = "./src/test/resources/config/multiIssuer/";

    private static final String BOOTSTRAP_PATH = BASE_PATH + "bootstrap.json";
    private static final String ACCESS_TOKEN_PATH = BASE_PATH + "access_token.json";
    private static final String ID_TOKEN_PATH = BASE_PATH + "id_token.json";
    private static final String USERINFO_TOKEN_PATH = BASE_PATH + "userinfo.json";
    private static final String RESOURCE_PATH = BASE_PATH + "resource.json";
    private static final String ACTION_PATH = BASE_PATH + "action.txt";

    @BeforeMethod
    public void setUp() throws Exception {
        adapter = new CedarlingAdapter();
        jwtCreator = new JWTCreator(JWT_SECRET);

        String bootstrapJson = readFile(BOOTSTRAP_PATH);
        adapter.loadFromJson(updatePolicyStorePathInBootstrap(bootstrapJson));

        assertNotNull(adapter.getCedarling(), "Cedarling instance should be initialized");

        action = readFile(ACTION_PATH);
        resource = new JSONObject(readFile(RESOURCE_PATH));
    }

    // ---------------------------
    // Helper Methods
    // ---------------------------

    private String updatePolicyStorePathInBootstrap(String bootstrapJsonString) throws JsonProcessingException {
        JsonNode bootstrapJson = mapper.readTree(bootstrapJsonString);

        File resourcesDirectory = new File("src/test/resources");
        String absolutePath = resourcesDirectory.getAbsolutePath() + "/config/multiIssuer/policy-store";

        ((ObjectNode) bootstrapJson).put("CEDARLING_POLICY_STORE_LOCAL_FN",
                absolutePath);
        return bootstrapJson.toString();
    }

    private String readFile(String path) throws Exception {
        return AppUtils.readFile(path);
    }

    private Map<String, String> createValidTokens() throws Exception {
        Map<String, String> tokens = new HashMap<>();
        tokens.put(ACCESS_TOKEN_KEY, jwtCreator.createJwtFromJson(readFile(ACCESS_TOKEN_PATH)));
        tokens.put(ID_TOKEN_KEY, jwtCreator.createJwtFromJson(readFile(ID_TOKEN_PATH)));
        tokens.put(USERINFO_TOKEN_KEY, jwtCreator.createJwtFromJson(readFile(USERINFO_TOKEN_PATH)));
        return tokens;
    }

    private JSONObject emptyContext() {
        return new JSONObject();
    }

    // ---------------------------
    // Authorization Tests
    // ---------------------------

    @Test
    public void testAuthorizeSuccess() throws Exception {
        MultiIssuerAuthorizeResult result =
                adapter.authorizeMultiIssuer(createValidTokens(), action, resource, emptyContext());

        assertNotNull(result, "Authorization result should not be null");
        assertNotNull(result.getRequestId(), "Request ID should not be null");
        assertTrue(result.getDecision(), "Authorization should ALLOW for valid tokens");
    }

    @Test(expectedExceptions = uniffi.cedarling_uniffi.AuthorizeException.class)
    public void testAuthorizeDeniedInvalidToken() throws Exception {
        Map<String, String> tokens = createValidTokens();
        tokens.put(ACCESS_TOKEN_KEY, "invalid.token");
        tokens.put(ID_TOKEN_KEY, "invalid.token");
        tokens.put(USERINFO_TOKEN_KEY, "invalid.token");

        MultiIssuerAuthorizeResult result =
                adapter.authorizeMultiIssuer(tokens, action, resource, emptyContext());
    }

    @Test(expectedExceptions = uniffi.cedarling_uniffi.AuthorizeException.class)
    public void testAuthorizeMissingToken() throws Exception {
        Map<String, String> tokens = new HashMap<>();
        tokens.put(ID_TOKEN_KEY, "dummy");

        adapter.authorizeMultiIssuer(tokens, action, resource, emptyContext());
    }

    // ---------------------------
    // Trusted Issuer Tests
    // ---------------------------

    @Test
    public void testTrustedIssuerLoading() {
        assertFalse(adapter.isTrustedIssuerLoadedByName("Jans"),
                "Issuer 'Jans' should not be loaded by name");

        assertTrue(adapter.isTrustedIssuerLoadedByIss("https://admin-ui-test.gluu.org"),
                "Issuer should be loaded by ISS");

        long total = adapter.totalIssuers();
        long loaded = adapter.loadedTrustedIssuersCount();

        assertTrue(loaded <= total, "Loaded issuers should not exceed total issuers");

        List<String> loadedIds = adapter.loadedTrustedIssuerIds();
        assertEquals(loadedIds.size(), loaded, "Loaded IDs count mismatch");

        for (String id : loadedIds) {
            assertTrue(adapter.isTrustedIssuerLoadedByName(id),
                    "Loaded issuer ID should be resolvable by name");
        }
    }

    // ---------------------------
    // Logging Tests
    // ---------------------------

    @Test(dependsOnMethods = "testAuthorizeSuccess")
    public void testGetLogIds() {
        List<String> logEntries = adapter.getLogIds();
        assertFalse(logEntries.isEmpty(), "Log IDs should not be empty");
    }

    @Test(dependsOnMethods = "testAuthorizeSuccess")
    public void testGetLogsByTag() throws LogException {
        assertFalse(adapter.getLogsByTag("System").isEmpty(),
                "Logs for 'System' tag should not be empty");
    }

    @Test(dependsOnMethods = "testAuthorizeSuccess")
    public void testPopLogs() throws LogException {
        List<String> logEntries = adapter.popLogs();
        assertFalse(logEntries.isEmpty(), "Popped logs should not be empty");
    }

    // ---------------------------
    // Data Context Tests
    // ---------------------------

    @Test
    public void testPushAndGetData() throws DataException {
        JSONObject value1 = new JSONObject().put("data", "value1");
        adapter.pushDataCtx("key1", value1);

        Object result1 = adapter.getDataCtx("key1");
        assertEquals(((JSONObject) result1).getString("data"), "value1");

        JSONObject value2 = new JSONObject().put("nested", "data");
        adapter.pushDataCtx("key2", value2, 60L);

        Object result2 = adapter.getDataCtx("key2");
        assertEquals(((JSONObject) result2).getString("nested"), "data");

        adapter.pushDataCtx("key3", "[1,2,3]", null);
        Object result3 = adapter.getDataCtx("key3");

        assertTrue(result3 instanceof JSONArray, "Expected JSONArray for array JSON input");
    }

    @Test
    public void testTTLExpiry() throws Exception {
        JSONObject value = new JSONObject().put("data", "temp");
        adapter.pushDataCtx("ttlKey", value, 1L);

        Thread.sleep(1500);

        assertNull(adapter.getDataCtx("ttlKey"), "Data should expire after TTL");
    }

    @Test
    public void testRemoveData() throws DataException {
        adapter.pushDataCtx("removeKey", new JSONObject().put("data", "value"));

        assertNotNull(adapter.getDataCtx("removeKey"));

        assertTrue(adapter.removeDataCtx("removeKey"), "Remove should return true");
        assertNull(adapter.getDataCtx("removeKey"), "Data should be removed");

        assertFalse(adapter.removeDataCtx("nonexistent"),
                "Removing non-existent key should return false");
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

        assertEquals(entries.size(), 2, "Expected 2 data entries");

        Set<String> keys = new HashSet<>();
        for (DataEntry entry : entries) {
            keys.add(entry.getKey());
        }

        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
    }

    @Test
    public void testGetStats() throws DataException {
        assertEquals(adapter.listDataCtx().size(), 0, "Initial data store should be empty");

        adapter.pushDataCtx("k1", new JSONObject());
        adapter.pushDataCtx("k2", new JSONObject());

        DataStoreStats stats = adapter.getStatsCtx();

        assertNotNull(stats, "Stats should not be null");
        assertEquals(adapter.listDataCtx().size(), 2, "Data store size mismatch");
        assertTrue(stats.getMetricsEnabled(), "Metrics should be enabled");
    }

    @Test(expectedExceptions = DataException.class)
    public void testInvalidKey() throws DataException {
        adapter.pushDataCtx("", new JSONObject());
    }
}