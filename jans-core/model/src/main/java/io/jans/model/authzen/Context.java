package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AuthZEN Context - completely unstructured key-value map.
 * Per AuthZEN spec, context is environmental data with no fixed schema.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Context {

    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public Context() {
        // empty
    }

    public Context(Map<String, Object> attributes) {
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    @JsonAnySetter
    public Context put(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public boolean containsKey(String key) {
        return attributes.containsKey(key);
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public String toString() {
        return "Context{" +
                "attributes=" + attributes +
                '}';
    }
}
