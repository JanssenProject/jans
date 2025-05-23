package io.jans.cedarling.binding.wrapper;

import io.jans.cedarling.binding.wrapper.jwt.JWTCreator;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.testng.annotations.*;
import org.json.JSONObject;

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
}
