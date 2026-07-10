package io.jans.cedarling.binding.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uniffi.cedarling_uniffi.AuthorizeException;
import uniffi.cedarling_uniffi.AuthorizeResult;
import uniffi.cedarling_uniffi.BatchAuthorizeUnsignedResponse;
import uniffi.cedarling_uniffi.BatchItem;
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
        for (AuthorizeResult r : response.getResults()) {
            assertTrue(r.getDecision(), "item should allow");
        }
    }

    @Test
    public void batchMatchesSingleItemResult() throws Exception {
        AuthorizeResult single = adapter.authorizeUnsigned(principalsString, action, resource, context);
        BatchAuthorizeUnsignedResponse batch =
                adapter.authorizeUnsignedBatch(principalsString, List.of(sameItem()));

        assertEquals(batch.getResults().size(), 1);
        assertEquals(
                batch.getResults().get(0).getDecision(),
                single.getDecision(),
                "single-item and batch decisions must agree on the same input");
    }

    @Test
    public void batchWithNullContextItemUsesDefault() throws Exception {
        BatchItem item = adapter.batchItemFromJson(resource, action, null);
        BatchAuthorizeUnsignedResponse response =
                adapter.authorizeUnsignedBatch(principalsString, List.of(item));

        assertEquals(response.getResults().size(), 1);
        assertTrue(response.getResults().get(0).getDecision());
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
}
