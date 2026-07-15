package io.jans.cedarling.binding.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.cedarling.binding.wrapper.jwt.JWTCreator;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uniffi.cedarling_uniffi.AuthorizeException;
import uniffi.cedarling_uniffi.BatchAuthorizeMultiIssuerResponse;
import uniffi.cedarling_uniffi.BatchItem;
import uniffi.cedarling_uniffi.EntityData;
import uniffi.cedarling_uniffi.EntityException;
import uniffi.cedarling_uniffi.MultiIssuerAuthorizeResult;
import uniffi.cedarling_uniffi.TokenInput;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Tests for {@code authorizeMultiIssuerBatch}. Reuses the multi-issuer policy
 * store and JWT fixtures from {@link MultiIssuerAuthzTest} so a batch of N
 * items with the same valid token set marshals through the UniFFI boundary
 * cleanly, produces the right per-item arity, and returns a non-empty
 * {@code batch_id}.
 */
public class MultiIssuerBatchAuthzTest {

    private CedarlingAdapter adapter;
    private JWTCreator jwtCreator;
    private String action;
    private JSONObject resource;
    ObjectMapper mapper = new ObjectMapper();

    private static final String JWT_SECRET =
            "-very-strong-shared-secret-of-at-least-32-bytes!";
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
        String bootstrapJson = AppUtils.readFile(BOOTSTRAP_PATH);
        adapter.loadFromJson(updatePolicyStorePathInBootstrap(bootstrapJson));
        assertNotNull(adapter.getCedarling(), "Cedarling should be initialized");

        action = AppUtils.readFile(ACTION_PATH);
        resource = new JSONObject(AppUtils.readFile(RESOURCE_PATH));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (adapter != null) {
            adapter.close();
        }
    }

    private String updatePolicyStorePathInBootstrap(String bootstrapJsonString)
            throws JsonProcessingException {
        JsonNode bootstrapJson = mapper.readTree(bootstrapJsonString);
        File resourcesDirectory = new File("src/test/resources");
        String absolutePath =
                resourcesDirectory.getAbsolutePath() + "/config/multiIssuer/policy-store";
        ((ObjectNode) bootstrapJson).put("CEDARLING_POLICY_STORE_LOCAL_FN", absolutePath);
        return bootstrapJson.toString();
    }

    private Map<String, String> validTokensMap() throws Exception {
        Map<String, String> tokens = new HashMap<>();
        tokens.put(ACCESS_TOKEN_KEY, jwtCreator.createJwtFromJson(AppUtils.readFile(ACCESS_TOKEN_PATH)));
        tokens.put(ID_TOKEN_KEY, jwtCreator.createJwtFromJson(AppUtils.readFile(ID_TOKEN_PATH)));
        tokens.put(USERINFO_TOKEN_KEY, jwtCreator.createJwtFromJson(AppUtils.readFile(USERINFO_TOKEN_PATH)));
        return tokens;
    }

    private BatchItem sameItem() throws EntityException {
        return adapter.batchItemFromJson(resource, action, new JSONObject());
    }

    @Test
    public void batchWithValidTokensAndThreeItemsAllows() throws Exception {
        List<BatchItem> items = new ArrayList<>();
        items.add(sameItem());
        items.add(sameItem());
        items.add(sameItem());

        BatchAuthorizeMultiIssuerResponse response =
                adapter.authorizeMultiIssuerBatch(validTokensMap(), items);

        assertNotNull(response, "response should not be null");
        assertNotNull(response.getBatchId(), "batch_id should not be null");
        assertFalse(response.getBatchId().isEmpty(), "batch_id should be populated");
        assertEquals(response.getResults().size(), 3, "N=3 items produce N=3 results");
        for (MultiIssuerAuthorizeResult r : response.getResults()) {
            assertTrue(r.getDecision(), "item should allow with valid multi-issuer tokens");
        }
    }

    @Test
    public void batchMatchesSingleItemResult() throws Exception {
        Map<String, String> tokens = validTokensMap();
        MultiIssuerAuthorizeResult single =
                adapter.authorizeMultiIssuer(tokens, action, resource, new JSONObject());
        BatchAuthorizeMultiIssuerResponse batch =
                adapter.authorizeMultiIssuerBatch(tokens, List.of(sameItem()));

        assertEquals(batch.getResults().size(), 1);
        assertEquals(
                batch.getResults().get(0).getDecision(),
                single.getDecision(),
                "single-item and batch decisions must agree on the same input");
    }

    @Test
    public void batchWithNullContextItemUsesDefault() throws Exception {
        BatchItem item = adapter.batchItemFromJson(resource, action, null);
        BatchAuthorizeMultiIssuerResponse response =
                adapter.authorizeMultiIssuerBatch(validTokensMap(), List.of(item));

        assertEquals(response.getResults().size(), 1);
        assertTrue(response.getResults().get(0).getDecision());
    }

    @Test
    public void batchWithTokenInputListOverloadAllows() throws Exception {
        // Exercise the List<TokenInput> overload (the Map<> overload is covered above).
        Map<String, String> tokenMap = validTokensMap();
        List<TokenInput> tokens = new ArrayList<>();
        for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
            tokens.add(new TokenInput(entry.getKey(), entry.getValue()));
        }

        BatchAuthorizeMultiIssuerResponse response =
                adapter.authorizeMultiIssuerBatch(tokens, List.of(sameItem()));

        assertEquals(response.getResults().size(), 1);
        assertTrue(response.getResults().get(0).getDecision());
    }

    @Test
    public void batchEmptyTokensRejected() throws Exception {
        AuthorizeException err = expectThrows(
                AuthorizeException.class,
                () -> adapter.authorizeMultiIssuerBatch(new HashMap<>(), List.of(sameItem())));
        assertNotNull(err.getMessage(), "error message should be present");
        assertTrue(
                err.getMessage().toLowerCase().contains("empty"),
                "error should mention empty tokens, got: " + err.getMessage());
    }

    @Test
    public void batchEmptyItemsRejected() throws Exception {
        AuthorizeException err = expectThrows(
                AuthorizeException.class,
                () -> adapter.authorizeMultiIssuerBatch(validTokensMap(), new ArrayList<>()));
        assertNotNull(err.getMessage(), "error message should be present");
        assertTrue(
                err.getMessage().toLowerCase().contains("empty"),
                "error should mention empty items, got: " + err.getMessage());
    }

    /**
     * Ordering: N=3 items where item[1] has a malformed action UID
     * (synthesizes a fail-closed Deny) sandwiched between two valid items.
     * Verifies each results[i] carries the decision produced by items[i]
     * rather than a uniform pass/fail across the batch.
     */
    @Test
    public void batchMixedDecisionsPreserveOrder() throws Exception {
        Map<String, String> tokens = validTokensMap();
        BatchItem badItem = new BatchItem(
                EntityData.Companion.fromJson(resource.toString()),
                "this is not a valid uid",
                new JSONObject().toString());
        List<BatchItem> items = new ArrayList<>();
        items.add(sameItem());
        items.add(badItem);
        items.add(sameItem());

        BatchAuthorizeMultiIssuerResponse response =
                adapter.authorizeMultiIssuerBatch(tokens, items);

        assertEquals(response.getResults().size(), 3, "N=3 items → N=3 results");
        assertTrue(response.getResults().get(0).getDecision(), "item 0 must allow");
        assertFalse(
                response.getResults().get(1).getDecision(),
                "item 1 with bad action must fail closed");
        assertTrue(response.getResults().get(2).getDecision(), "item 2 must allow");
    }
}
