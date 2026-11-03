package io.jans.cedarling.binding.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.cedarling.binding.wrapper.jwt.JWTCreator;
import io.jans.cedarling.binding.wrapper.utils.AppUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uniffi.cedarling_uniffi.AuthorizeOptions;
import uniffi.cedarling_uniffi.BatchAuthorizeMultiIssuerResponse;
import uniffi.cedarling_uniffi.BatchAuthorizeUnsignedResponse;
import uniffi.cedarling_uniffi.BatchItem;
import uniffi.cedarling_uniffi.EntityData;
import uniffi.cedarling_uniffi.TraceContext;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * {@code AuthorizeOptions} travel with a request but never take part in the
 * Cedar evaluation, so every {@code authorize*} gives the same decision with
 * and without them, and {@code null} options behave as the old overloads.
 */
public class AuthorizeOptionsTest {

    private static final String UNSIGNED_PATH = "./src/test/resources/config/unsigned/";
    private static final String MULTI_ISSUER_PATH = "./src/test/resources/config/multiIssuer/";
    private static final String JWT_SECRET =
            "-very-strong-shared-secret-of-at-least-32-bytes!";

    private final ObjectMapper mapper = new ObjectMapper();
    private CedarlingAdapter adapter;

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (adapter != null) {
            adapter.close();
        }
    }

    private static AuthorizeOptions traceOptions() {
        return new AuthorizeOptions(new TraceContext(
                "exec-1", "https://gateway.example.com", "workload-1", "workload-1-a"));
    }

    private CedarlingAdapter load(String basePath, String storeDir) throws Exception {
        ObjectNode bootstrap =
                (ObjectNode) mapper.readTree(AppUtils.readFile(basePath + "bootstrap.json"));
        String store = new File("src/test/resources/config/" + storeDir + "/policy-store")
                .getAbsolutePath();
        bootstrap.put("CEDARLING_POLICY_STORE_LOCAL_FN", store);
        CedarlingAdapter loaded = new CedarlingAdapter();
        loaded.loadFromJson(bootstrap.toString());
        assertNotNull(loaded.getCedarling(), "Cedarling should be initialized");
        return loaded;
    }

    private Map<String, String> multiIssuerTokens() throws Exception {
        JWTCreator jwt = new JWTCreator(JWT_SECRET);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("Jans::Access_token",
                jwt.createJwtFromJson(AppUtils.readFile(MULTI_ISSUER_PATH + "access_token.json")));
        tokens.put("Jans::id_token",
                jwt.createJwtFromJson(AppUtils.readFile(MULTI_ISSUER_PATH + "id_token.json")));
        tokens.put("Jans::Userinfo_token",
                jwt.createJwtFromJson(AppUtils.readFile(MULTI_ISSUER_PATH + "userinfo.json")));
        return tokens;
    }

    @Test
    public void authorizeUnsignedWithOptionsMatchesPlain() throws Exception {
        adapter = load(UNSIGNED_PATH, "unsigned");
        String principal = AppUtils.readFile(UNSIGNED_PATH + "principals.json");
        String action = AppUtils.readFile(UNSIGNED_PATH + "action.txt");
        JSONObject resource = new JSONObject(AppUtils.readFile(UNSIGNED_PATH + "resource.json"));
        JSONObject context = new JSONObject(AppUtils.readFile(UNSIGNED_PATH + "context.json"));

        boolean plain = adapter.authorizeUnsigned(principal, action, resource, context).getDecision();
        assertTrue(plain, "baseline unsigned request should allow");
        assertEquals(
                adapter.authorizeUnsigned(principal, action, resource, context, traceOptions())
                        .getDecision(),
                plain,
                "options must not change the unsigned decision");
        assertEquals(
                adapter.authorizeUnsignedEntity(
                                EntityData.Companion.fromJson(principal),
                                action, resource, context, traceOptions())
                        .getDecision(),
                plain,
                "options must not change the decision for an EntityData principal");

        List<BatchItem> items = List.of(adapter.batchItemFromJson(resource, action, context));
        BatchAuthorizeUnsignedResponse batch =
                adapter.authorizeUnsignedBatch(principal, items, traceOptions());
        assertEquals(batch.getResults().size(), 1, "one item gives one result");
        assertEquals(
                CedarlingAdapter.unwrap(batch.getResults().get(0)).getDecision(),
                plain,
                "options must not change the unsigned batch decision");
    }

    @Test
    public void authorizeMultiIssuerBatchWithOptions() throws Exception {
        adapter = load(MULTI_ISSUER_PATH, "multiIssuer");
        Map<String, String> tokens = multiIssuerTokens();
        String action = AppUtils.readFile(MULTI_ISSUER_PATH + "action.txt");
        JSONObject resource =
                new JSONObject(AppUtils.readFile(MULTI_ISSUER_PATH + "resource.json"));
        List<BatchItem> items = List.of(adapter.batchItemFromJson(resource, action, null));

        boolean plain = CedarlingAdapter.unwrap(
                adapter.authorizeMultiIssuerBatch(tokens, items).getResults().get(0))
                .getDecision();
        assertTrue(plain, "baseline multi-issuer batch item should allow");

        BatchAuthorizeMultiIssuerResponse traced =
                adapter.authorizeMultiIssuerBatch(tokens, items, traceOptions());
        assertEquals(traced.getResults().size(), 1, "one item gives one result");
        assertEquals(
                CedarlingAdapter.unwrap(traced.getResults().get(0)).getDecision(),
                plain,
                "options must not change the multi-issuer batch decision");
        assertEquals(
                adapter.authorizeMultiIssuer(tokens, action, resource, null, traceOptions())
                        .getDecision(),
                plain,
                "options must not change the multi-issuer decision");
    }

    @Test
    public void nullOptionsBehaveAsBefore() throws Exception {
        adapter = load(UNSIGNED_PATH, "unsigned");
        String principal = AppUtils.readFile(UNSIGNED_PATH + "principals.json");
        String action = AppUtils.readFile(UNSIGNED_PATH + "action.txt");
        JSONObject resource = new JSONObject(AppUtils.readFile(UNSIGNED_PATH + "resource.json"));
        JSONObject context = new JSONObject(AppUtils.readFile(UNSIGNED_PATH + "context.json"));

        assertEquals(
                adapter.authorizeUnsigned(principal, action, resource, context, null)
                        .getDecision(),
                adapter.authorizeUnsigned(principal, action, resource, context).getDecision(),
                "null options should behave as the overload without options");
    }
}
