package io.jans.model.authzen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests for AuthZEN Context model.
 * Context is a completely unstructured key-value map per AuthZEN spec.
 *
 * @author Yuriy Z
 */
public class ContextTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void context_whenCreatedEmpty_shouldBeEmpty() {
        Context context = new Context();
        assertTrue(context.isEmpty());
        assertTrue(context.getAttributes().isEmpty());
    }

    @Test
    public void context_whenAddingAttributes_shouldStoreThemCorrectly() {
        Context context = new Context();
        context.put("time", "2024-01-15T10:30:00Z");
        context.put("ip_address", "192.168.1.1");
        context.put("count", 42);

        assertEquals(context.get("time"), "2024-01-15T10:30:00Z");
        assertEquals(context.get("ip_address"), "192.168.1.1");
        assertEquals(context.get("count"), 42);
        assertFalse(context.isEmpty());
    }

    @Test
    public void context_whenDeserializingFromJson_shouldParseDynamicFields() throws Exception {
        String json = "{\"time\":\"2024-01-15T10:30:00Z\",\"location\":\"US\",\"device_id\":\"abc123\"}";

        Context context = objectMapper.readValue(json, Context.class);

        assertEquals(context.get("time"), "2024-01-15T10:30:00Z");
        assertEquals(context.get("location"), "US");
        assertEquals(context.get("device_id"), "abc123");
    }

    @Test
    public void context_whenSerializingToJson_shouldProduceFlatStructure() throws Exception {
        Context context = new Context();
        context.put("time", "2024-01-15T10:30:00Z");
        context.put("location", "US");

        String json = objectMapper.writeValueAsString(context);

        assertTrue(json.contains("\"time\":\"2024-01-15T10:30:00Z\""));
        assertTrue(json.contains("\"location\":\"US\""));
        // Should NOT have nested "attributes" wrapper
        assertFalse(json.contains("\"attributes\""));
    }

    @Test
    public void context_whenCreatedWithMap_shouldContainAllEntries() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key1", "value1");
        attrs.put("key2", 123);

        Context context = new Context(attrs);

        assertEquals(context.get("key1"), "value1");
        assertEquals(context.get("key2"), 123);
    }

    @Test
    public void context_whenCheckingContainsKey_shouldReturnCorrectResult() {
        Context context = new Context();
        context.put("existing_key", "value");

        assertTrue(context.containsKey("existing_key"));
        assertFalse(context.containsKey("non_existing_key"));
    }

    @Test
    public void context_fluentApi_shouldSupportChaining() {
        Context context = new Context()
                .put("key1", "value1")
                .put("key2", "value2");

        assertEquals(context.get("key1"), "value1");
        assertEquals(context.get("key2"), "value2");
    }

    @Test
    public void context_withNestedObject_shouldSerializeCorrectly() throws Exception {
        Context context = new Context();
        Map<String, Object> nested = new HashMap<>();
        nested.put("innerKey", "innerValue");
        context.put("nested", nested);

        String json = objectMapper.writeValueAsString(context);

        assertTrue(json.contains("\"nested\""));
        assertTrue(json.contains("\"innerKey\""));
    }
}
