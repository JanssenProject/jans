package io.jans.cedarling.binding.wrapper;

import io.jans.cedarling.binding.wrapper.jwt.JWTCreator;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.testng.annotations.*;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.*;

import uniffi.cedarling_uniffi.*;
import static org.testng.Assert.*;

public class CedarlingAdapterTest {

    private CedarlingAdapter adapter;
    private JWTCreator jwtCreator;
    private AuthorizeResult result;

    public static final String JWT_SECRET = "-very-strong-shared-secret-of-at-least-32-bytes!";
    public static final String BOOTSTRAP_JSON_PATH = "./src/test/resources/config/bootstrap.json";
    public static final String ACCESS_TOKEN_FILE_PATH = "./src/test/resources/config/access_token_payload.json";
    public static final String ID_TOKEN_FILE_PATH = "./src/test/resources/config/id_token_payload.json";
    public static final String USER_INFO_FILE_PATH = "./src/test/resources/config/user_info_payload.json";

    public static final String ACTION_FILE_PATH = "./src/test/resources/config/action.txt";
    public static final String RESOURCE_FILE_PATH = "./src/test/resources/config/resource.json";
    public static final String CONTEXT_FILE_PATH = "./src/test/resources/config/context.json";
    public static final String PRINCIPALS_FILE_PATH = "./src/test/resources/config/principals.json";

    @BeforeMethod
    public void setUp() throws Exception {
        adapter = new CedarlingAdapter();
        jwtCreator = new JWTCreator(JWT_SECRET);
        // Load Cedarling bootstrap configuration from file
        String bootstrapJson = AppUtils.readFile(BOOTSTRAP_JSON_PATH);
        adapter.loadFromJson(bootstrapJson);
        assertNotNull(adapter.getCedarling());
    }

    @Test
    public void testAuthorize() throws Exception {
        // Read JWT payloads from files
        String accessTokenPayload = AppUtils.readFile(ACCESS_TOKEN_FILE_PATH);
        String idTokenPayload = AppUtils.readFile(ID_TOKEN_FILE_PATH);
        String userInfoPayload = AppUtils.readFile(USER_INFO_FILE_PATH);
        // Read input files for authorization
        String action = AppUtils.readFile(ACTION_FILE_PATH);
        String resourceJson = AppUtils.readFile(RESOURCE_FILE_PATH);
        String contextJson = AppUtils.readFile(CONTEXT_FILE_PATH);
        // Generate signed JWTs from the payloads
        Map<String, String> tokens = Map.of(
                "access_token", jwtCreator.createJwtFromJson(accessTokenPayload),
                "id_token", jwtCreator.createJwtFromJson(idTokenPayload),
                "userinfo_token", jwtCreator.createJwtFromJson(userInfoPayload)
        );

        // Perform authorization
        result = adapter.authorize(tokens, action, new JSONObject(resourceJson), new JSONObject(contextJson));

        assertNotNull(result);
        assertNotNull(result.getPerson());
        assertNotNull(result.getWorkload());
        assertTrue(result.getDecision());
        assertNotEquals(adapter.getLogsByRequestIdAndTag(result.getRequestId(), "System").size(), 0);
        assertNotEquals(adapter.getLogsByRequestId(result.getRequestId()).size(), 0);
    }

    @Test(dependsOnMethods = {"testAuthorize"})
    public void testGetLogById() throws LogException {
        List<String> logEntrys = adapter.getLogIds();
        String logEntry = adapter.getLogById(logEntrys.get(0));
        assertNotNull(logEntry);
    }

    @Test(dependsOnMethods = {"testAuthorize"})
    public void testGetLogIds() {
        List<String> logEntrys = adapter.getLogIds();
        assertNotEquals(logEntrys.size(), 0);
    }

    @Test
    public void testGetLogsByTag() throws LogException {
        assertNotEquals(adapter.getLogsByTag("System").size(), 0);
    }

    @Test
    public void testPopLogs() throws LogException {
        List<String> logEntrys = adapter.popLogs();
        assertNotEquals(logEntrys.size(), 0);
    }

    @Test
    public void testAuthorizeUnsigned() throws Exception {
        // Read input files for authorization
        String action = AppUtils.readFile(ACTION_FILE_PATH);
        String resourceJson = AppUtils.readFile(RESOURCE_FILE_PATH);
        String contextJson = AppUtils.readFile(CONTEXT_FILE_PATH);
        String principalsString = AppUtils.readFile(PRINCIPALS_FILE_PATH);

        List<EntityData> principals = List.of(EntityData.Companion.fromJson(principalsString));

        JSONObject resource = new JSONObject(resourceJson);
        JSONObject context = new JSONObject(contextJson);

        AuthorizeResult result = adapter.authorizeUnsigned(principals, action, resource, context);
        assertNotNull(result);
        assertNotNull(result.getPrincipals());
        assertEquals(result.getDecision(), false);
    }

    @Test
    public void testLoadFromFile() throws CedarlingException {
        adapter.loadFromFile(BOOTSTRAP_JSON_PATH);
        assertNotNull(adapter.getCedarling());
    }

    @Test
    public void testPushAndGetData() throws DataException {
        // Push data without TTL
        JSONObject value1 = new JSONObject();
        value1.put("data", "value1");
        adapter.pushDataCtx("key1", value1);
        JSONObject result1 = adapter.getDataCtx("key1");
        assertNotNull(result1);
        assertEquals(result1.getString("data"), "value1");

        // Push data with TTL
        JSONObject value2 = new JSONObject();
        value2.put("nested", "data");
        adapter.pushDataCtx("key2", value2, 60L);
        JSONObject result2 = adapter.getDataCtx("key2");
        assertNotNull(result2);
        assertEquals(result2.getString("nested"), "data");

        // Push array as JSON string - getDataCtx should throw JSONException when trying to parse non-object JSON
        String arrayJson = "[1, 2, 3]";
        adapter.pushDataCtx("key3", arrayJson, null);
        try {
            adapter.getDataCtx("key3");
            fail("Expected JSONException when calling getDataCtx on non-object JSON");
        } catch (JSONException e) {
            // Expected: JSONObject constructor throws JSONException when parsing array string
            assertNotNull(e);
        }
    }

    @Test
    public void testGetDataEntry() throws DataException {
        JSONObject value = new JSONObject();
        value.put("foo", "bar");
        adapter.pushDataCtx("test_key", value);

        DataEntry entry = adapter.getDataEntryCtx("test_key");
        assertNotNull(entry);
        assertEquals(entry.getKey(), "test_key");
        // JsonValue is a custom_newtype wrapping String in UniFFI, so it returns String directly
        String entryValue = entry.getValue();
        assertNotNull(entryValue);
        assertNotNull(entry.getDataType());
        assertNotNull(entry.getCreatedAt());
        assertNotNull(entry.getExpiresAt());
    }

    @Test
    public void testRemoveData() throws DataException {
        JSONObject value = new JSONObject();
        value.put("data", "to_remove");
        adapter.pushDataCtx("to_remove", value);

        JSONObject result = adapter.getDataCtx("to_remove");
        assertNotNull(result);
        assertEquals(result.getString("data"), "to_remove");

        boolean removed = adapter.removeDataCtx("to_remove");
        assertTrue(removed);

        JSONObject resultAfter = adapter.getDataCtx("to_remove");
        assertNull(resultAfter);

        // Try removing non-existent key
        boolean removedNonExistent = adapter.removeDataCtx("non_existent");
        assertFalse(removedNonExistent);
    }

    @Test
    public void testClearData() throws DataException {
        JSONObject value1 = new JSONObject();
        value1.put("data", "value1");
        adapter.pushDataCtx("key1", value1);

        JSONObject value2 = new JSONObject();
        value2.put("data", "value2");
        adapter.pushDataCtx("key2", value2);

        JSONObject value3 = new JSONObject();
        value3.put("data", "value3");
        adapter.pushDataCtx("key3", value3);

        assertNotNull(adapter.getDataCtx("key1"));
        assertNotNull(adapter.getDataCtx("key2"));
        assertNotNull(adapter.getDataCtx("key3"));

        adapter.clearDataCtx();

        assertNull(adapter.getDataCtx("key1"));
        assertNull(adapter.getDataCtx("key2"));
        assertNull(adapter.getDataCtx("key3"));
    }

    @Test
    public void testListData() throws DataException {
        JSONObject value1 = new JSONObject();
        value1.put("data", "value1");
        adapter.pushDataCtx("key1", value1);

        JSONObject value2 = new JSONObject();
        value2.put("nested", "data");
        adapter.pushDataCtx("key2", value2);

        String arrayJson = "[1, 2, 3]";
        adapter.pushDataCtx("key3", arrayJson, null);

        List<DataEntry> entries = adapter.listDataCtx();
        assertNotNull(entries);
        assertEquals(3, entries.size());

        Set<String> keys = new HashSet<>();
        for (DataEntry entry : entries) {
            keys.add(entry.getKey());
        }
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));
    }

    @Test
    public void testGetStats() throws DataException {
        DataStoreStats stats = adapter.getStatsCtx();
        assertNotNull(stats);
        assertEquals(adapter.listDataCtx().size(), 0);

        JSONObject value1 = new JSONObject();
        value1.put("data", "value1");
        adapter.pushDataCtx("key1", value1);

        JSONObject value2 = new JSONObject();
        value2.put("data", "value2");
        adapter.pushDataCtx("key2", value2);

        DataStoreStats statsAfter = adapter.getStatsCtx();
        assertNotNull(statsAfter);
        assertEquals(adapter.listDataCtx().size(), 2);
        assertTrue(statsAfter.getMetricsEnabled());
    }

    @Test(expectedExceptions = DataException.class)
    public void testDataExceptionInvalidKey() throws DataException {
        // Empty key should throw DataException
        JSONObject value = new JSONObject();
        value.put("data", "value");
        adapter.pushDataCtx("", value);
    }
}
