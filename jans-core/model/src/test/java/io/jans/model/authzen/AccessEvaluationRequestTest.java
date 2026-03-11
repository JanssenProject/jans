package io.jans.model.authzen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.*;

/**
 * Tests for AuthZEN AccessEvaluationRequest model.
 * Used for both single evaluation and batch evaluations requests.
 *
 * @author Yuriy Z
 */
public class AccessEvaluationRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void accessEvaluationRequest_whenCreatedEmpty_shouldHaveNullFields() {
        AccessEvaluationRequest request = new AccessEvaluationRequest();

        assertNull(request.getSubject());
        assertNull(request.getResource());
        assertNull(request.getAction());
        assertNull(request.getContext());
        assertNull(request.getEvaluations());
        assertNull(request.getOptions());
    }

    @Test
    public void accessEvaluationRequest_whenUsingSetters_shouldStoreValuesCorrectly() {
        AccessEvaluationRequest request = new AccessEvaluationRequest();
        Subject subject = new Subject().setType("user").setId("123");
        Resource resource = new Resource().setType("document").setId("456");
        Action action = new Action().setName("read");
        Context context = new Context().put("ip", "192.168.1.1");

        request.setSubject(subject);
        request.setResource(resource);
        request.setAction(action);
        request.setContext(context);

        assertEquals(request.getSubject().getId(), "123");
        assertEquals(request.getResource().getId(), "456");
        assertEquals(request.getAction().getName(), "read");
        assertEquals(request.getContext().get("ip"), "192.168.1.1");
    }

    @Test
    public void accessEvaluationRequest_fluentApi_shouldSupportChaining() {
        AccessEvaluationRequest request = new AccessEvaluationRequest()
                .setSubject(new Subject().setType("user").setId("123"))
                .setResource(new Resource().setType("document").setId("456"))
                .setAction(new Action().setName("read"))
                .setContext(new Context().put("key", "value"));

        assertEquals(request.getSubject().getId(), "123");
        assertEquals(request.getResource().getId(), "456");
        assertEquals(request.getAction().getName(), "read");
        assertEquals(request.getContext().get("key"), "value");
    }

    @Test
    public void accessEvaluationRequest_withConstructor_shouldInitializeAllFields() {
        Subject subject = new Subject().setType("user").setId("123");
        Resource resource = new Resource().setType("document").setId("456");
        Action action = new Action().setName("write");
        Context context = new Context().put("time", "now");
        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.EXECUTE_ALL);

        AccessEvaluationRequest eval1 = new AccessEvaluationRequest();
        eval1.setAction(new Action().setName("read"));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                subject, resource, action, context,
                Collections.singletonList(eval1), options
        );

        assertEquals(request.getSubject().getId(), "123");
        assertEquals(request.getResource().getId(), "456");
        assertEquals(request.getAction().getName(), "write");
        assertEquals(request.getContext().get("time"), "now");
        assertEquals(request.getEvaluations().size(), 1);
        assertEquals(request.getOptions().getEvaluationsSemantic(), EvaluationOptions.EXECUTE_ALL);
    }

    @Test
    public void accessEvaluationRequest_whenDeserializingSingleEvaluation_shouldParseCorrectly() throws Exception {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"alice\"}," +
                "\"resource\": {\"type\": \"document\", \"id\": \"doc123\"}," +
                "\"action\": {\"name\": \"read\"}" +
                "}";

        AccessEvaluationRequest request = objectMapper.readValue(json, AccessEvaluationRequest.class);

        assertEquals(request.getSubject().getType(), "user");
        assertEquals(request.getSubject().getId(), "alice");
        assertEquals(request.getResource().getType(), "document");
        assertEquals(request.getResource().getId(), "doc123");
        assertEquals(request.getAction().getName(), "read");
        assertNull(request.getEvaluations());
        assertNull(request.getOptions());
    }

    @Test
    public void accessEvaluationRequest_whenDeserializingBatchEvaluations_shouldParseCorrectly() throws Exception {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"default-user\"}," +
                "\"resource\": {\"type\": \"resource\", \"id\": \"default-resource\"}," +
                "\"evaluations\": [" +
                "  {\"action\": {\"name\": \"read\"}}," +
                "  {\"action\": {\"name\": \"write\"}}," +
                "  {\"subject\": {\"type\": \"admin\", \"id\": \"admin1\"}, \"action\": {\"name\": \"delete\"}}" +
                "]," +
                "\"options\": {\"evaluations_semantic\": \"deny_on_first_deny\"}" +
                "}";

        AccessEvaluationRequest request = objectMapper.readValue(json, AccessEvaluationRequest.class);

        assertEquals(request.getSubject().getId(), "default-user");
        assertEquals(request.getResource().getId(), "default-resource");
        assertNotNull(request.getEvaluations());
        assertEquals(request.getEvaluations().size(), 3);
        assertEquals(request.getEvaluations().get(0).getAction().getName(), "read");
        assertEquals(request.getEvaluations().get(1).getAction().getName(), "write");
        assertEquals(request.getEvaluations().get(2).getSubject().getId(), "admin1");
        assertEquals(request.getEvaluations().get(2).getAction().getName(), "delete");
        assertNotNull(request.getOptions());
        assertEquals(request.getOptions().getEvaluationsSemantic(), "deny_on_first_deny");
    }

    @Test
    public void accessEvaluationRequest_whenSerializingSingleEvaluation_shouldProduceCorrectJson() throws Exception {
        AccessEvaluationRequest request = new AccessEvaluationRequest()
                .setSubject(new Subject().setType("user").setId("bob"))
                .setResource(new Resource().setType("file").setId("file789"))
                .setAction(new Action().setName("edit"));

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"type\":\"user\""));
        assertTrue(json.contains("\"id\":\"bob\""));
        assertTrue(json.contains("\"type\":\"file\""));
        assertTrue(json.contains("\"id\":\"file789\""));
        assertTrue(json.contains("\"name\":\"edit\""));
    }

    @Test
    public void accessEvaluationRequest_whenSerializingBatchEvaluations_shouldProduceCorrectJson() throws Exception {
        AccessEvaluationRequest eval1 = new AccessEvaluationRequest()
                .setAction(new Action().setName("read"));
        AccessEvaluationRequest eval2 = new AccessEvaluationRequest()
                .setAction(new Action().setName("write"));

        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.PERMIT_ON_FIRST_PERMIT);

        AccessEvaluationRequest request = new AccessEvaluationRequest()
                .setSubject(new Subject().setType("user").setId("default"))
                .setResource(new Resource().setType("doc").setId("default-doc"));
        request.setEvaluations(Arrays.asList(eval1, eval2));
        request.setOptions(options);

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"evaluations\""));
        assertTrue(json.contains("\"read\""));
        assertTrue(json.contains("\"write\""));
        assertTrue(json.contains("\"evaluations_semantic\":\"permit_on_first_permit\""));
    }

    @Test
    public void accessEvaluationRequest_withContext_shouldSerializeAndDeserializeCorrectly() throws Exception {
        String json = "{" +
                "\"subject\": {\"type\": \"user\", \"id\": \"alice\"}," +
                "\"resource\": {\"type\": \"api\", \"id\": \"/users\"}," +
                "\"action\": {\"name\": \"GET\"}," +
                "\"context\": {\"ip_address\": \"10.0.0.1\", \"time\": \"2024-01-15T10:30:00Z\"}" +
                "}";

        AccessEvaluationRequest request = objectMapper.readValue(json, AccessEvaluationRequest.class);

        assertNotNull(request.getContext());
        assertEquals(request.getContext().get("ip_address"), "10.0.0.1");
        assertEquals(request.getContext().get("time"), "2024-01-15T10:30:00Z");
    }

    @Test
    public void accessEvaluationRequest_toString_shouldContainAllFields() {
        AccessEvaluationRequest request = new AccessEvaluationRequest()
                .setSubject(new Subject().setType("user").setId("123"))
                .setResource(new Resource().setType("doc").setId("456"))
                .setAction(new Action().setName("read"));

        String str = request.toString();

        assertTrue(str.contains("subject="));
        assertTrue(str.contains("resource="));
        assertTrue(str.contains("action="));
        assertTrue(str.contains("context="));
        assertTrue(str.contains("evaluations="));
        assertTrue(str.contains("options="));
    }

    @Test
    public void accessEvaluationRequest_setEvaluations_shouldAllowEmptyList() {
        AccessEvaluationRequest request = new AccessEvaluationRequest();
        request.setEvaluations(Collections.emptyList());

        assertNotNull(request.getEvaluations());
        assertTrue(request.getEvaluations().isEmpty());
    }

    @Test
    public void accessEvaluationRequest_roundTrip_shouldPreserveAllData() throws Exception {
        AccessEvaluationRequest eval = new AccessEvaluationRequest()
                .setAction(new Action().setName("execute"));

        EvaluationOptions options = new EvaluationOptions();
        options.setEvaluationsSemantic(EvaluationOptions.DENY_ON_FIRST_DENY);

        AccessEvaluationRequest original = new AccessEvaluationRequest()
                .setSubject(new Subject().setType("service").setId("svc-001"))
                .setResource(new Resource().setType("endpoint").setId("/api/data"))
                .setAction(new Action().setName("POST"))
                .setContext(new Context().put("region", "us-east-1"));
        original.setEvaluations(Collections.singletonList(eval));
        original.setOptions(options);

        String json = objectMapper.writeValueAsString(original);
        AccessEvaluationRequest deserialized = objectMapper.readValue(json, AccessEvaluationRequest.class);

        assertEquals(deserialized.getSubject().getType(), "service");
        assertEquals(deserialized.getSubject().getId(), "svc-001");
        assertEquals(deserialized.getResource().getType(), "endpoint");
        assertEquals(deserialized.getResource().getId(), "/api/data");
        assertEquals(deserialized.getAction().getName(), "POST");
        assertEquals(deserialized.getContext().get("region"), "us-east-1");
        assertEquals(deserialized.getEvaluations().size(), 1);
        assertEquals(deserialized.getEvaluations().get(0).getAction().getName(), "execute");
        assertEquals(deserialized.getOptions().getEvaluationsSemantic(), EvaluationOptions.DENY_ON_FIRST_DENY);
    }
}
