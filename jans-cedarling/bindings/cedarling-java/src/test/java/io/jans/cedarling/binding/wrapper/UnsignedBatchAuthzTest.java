package io.jans.cedarling.binding.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uniffi.cedarling_uniffi.AuthorizeException;
import uniffi.cedarling_uniffi.AuthorizeResult;
import uniffi.cedarling_uniffi.BatchAuthorizeUnsignedResponse;
import uniffi.cedarling_uniffi.BatchItem;
import uniffi.cedarling_uniffi.BatchItemError;
import uniffi.cedarling_uniffi.BatchItemUnsignedOutcome;
import uniffi.cedarling_uniffi.EntityData;
import uniffi.cedarling_uniffi.EntityException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;


/**
 * Tests for {@code authorizeUnsignedBatch}. Reuses the same policy store and
 * principals fixture as {@link UnsignedAuthzTest}, so a batch of N=3 items
 * with the same allowing principal must all Allow, mirror the single-item
 * result exactly, and return a non-empty {@code batch_id}.
 */
public class UnsignedBatchAuthzTest {

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
        String bootstrapJson = AppUtils.readFile(BOOTSTRAP_PATH);
        adapter.loadFromJson(updatePolicyStorePathInBootstrap(bootstrapJson));
        assertNotNull(adapter.getCedarling(), "Cedarling should be initialized");

        action = AppUtils.readFile(ACTION_PATH);
        resource = new JSONObject(AppUtils.readFile(RESOURCE_PATH));
        context = new JSONObject(AppUtils.readFile(CONTEXT_PATH));
        principalsString = AppUtils.readFile(PRINCIPALS_PATH);
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
                resourcesDirectory.getAbsolutePath() + "/config/unsigned/policy-store";
        ((ObjectNode) bootstrapJson).put("CEDARLING_POLICY_STORE_LOCAL_FN", absolutePath);
        return bootstrapJson.toString();
    }

    private BatchItem sameItem() throws EntityException {
        return adapter.batchItemFromJson(resource, action, context);
    }

    @Test
    public void batchWithPrincipalsAndThreeItemsAllows() throws Exception {
        List<BatchItem> items = new ArrayList<>();
        items.add(sameItem());
        items.add(sameItem());
        items.add(sameItem());

        BatchAuthorizeUnsignedResponse response =
                adapter.authorizeUnsignedBatch(principalsString, items);

        assertNotNull(response, "response should not be null");
        assertNotNull(response.getBatchId(), "batch_id should not be null");
        assertFalse(response.getBatchId().isEmpty(), "batch_id should be populated");
        assertEquals(response.getResults().size(), 3, "N=3 items produce N=3 results");
        for (BatchItemUnsignedOutcome r : response.getResults()) {
            assertTrue(CedarlingAdapter.isOk(r), "item should be Ok");
            assertTrue(CedarlingAdapter.unwrap(r).getDecision(), "item should allow");
        }
    }

    @Test
    public void batchMatchesSingleItemResult() throws Exception {
        AuthorizeResult single = adapter.authorizeUnsigned(principalsString, action, resource, context);
        BatchAuthorizeUnsignedResponse batch =
                adapter.authorizeUnsignedBatch(principalsString, List.of(sameItem()));

        assertEquals(batch.getResults().size(), 1);
        assertTrue(CedarlingAdapter.isOk(batch.getResults().get(0)));
        assertEquals(
                CedarlingAdapter.unwrap(batch.getResults().get(0)).getDecision(),
                single.getDecision(),
                "single-item and batch decisions must agree on the same input");
    }

    @Test
    public void batchWithNullContextItemUsesDefault() throws Exception {
        BatchItem item = adapter.batchItemFromJson(resource, action, null);
        BatchAuthorizeUnsignedResponse response =
                adapter.authorizeUnsignedBatch(principalsString, List.of(item));

        assertEquals(response.getResults().size(), 1);
        assertTrue(CedarlingAdapter.isOk(response.getResults().get(0)));
        assertTrue(CedarlingAdapter.unwrap(response.getResults().get(0)).getDecision());
    }

    @Test
    public void batchEmptyItemsRejected() throws EntityException {
        AuthorizeException err = expectThrows(
                AuthorizeException.class,
                () -> adapter.authorizeUnsignedBatch(principalsString, new ArrayList<>()));
        assertNotNull(err.getMessage(), "error message should be present");
        assertTrue(
                err.getMessage().toLowerCase().contains("empty"),
                "error should mention empty items, got: " + err.getMessage());
    }

    /**
     * Ordering: N=3 items where item[1] has a malformed action UID (surfaces as
     * a Failed outcome) sandwiched between two valid items. Verifies each
     * results[i] carries the outcome produced by items[i] rather than a
     * uniform pass/fail across the batch.
     */
    @Test
    public void batchMixedDecisionsPreserveOrder() throws Exception {
        List<BatchItem> items = new ArrayList<>();
        items.add(sameItem());
        items.add(new BatchItem(
                EntityData.Companion.fromJson(resource.toString()),
                "this is not a valid uid",
                context.toString()));
        items.add(sameItem());

        BatchAuthorizeUnsignedResponse response =
                adapter.authorizeUnsignedBatch(principalsString, items);

        assertEquals(response.getResults().size(), 3, "N=3 items → N=3 results");
        assertTrue(CedarlingAdapter.isOk(response.getResults().get(0)), "item 0 must be Ok");
        assertTrue(
                CedarlingAdapter.unwrap(response.getResults().get(0)).getDecision(),
                "item 0 must allow");
        assertFalse(
                CedarlingAdapter.isOk(response.getResults().get(1)),
                "item 1 with bad action must be Failed");
        BatchItemError err = CedarlingAdapter.getError(response.getResults().get(1));
        assertNotNull(err, "item 1 must carry a BatchItemError");
        assertEquals(err.getCategory(), "action_parse");
        assertEquals(err.getItemIndex(), 1L);
        assertTrue(CedarlingAdapter.isOk(response.getResults().get(2)), "item 2 must be Ok");
        assertTrue(
                CedarlingAdapter.unwrap(response.getResults().get(2)).getDecision(),
                "item 2 must allow");
    }
}
