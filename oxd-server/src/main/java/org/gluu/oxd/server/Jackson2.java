package org.gluu.oxd.server;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxd.common.CoreUtils;

import java.io.IOException;

/**
 * Sticks to jackson 2 (2.9.5). We got this problem due to migration to dropwizard 1.3.1 which is using jackson 2.9.5
 *
 * @author yuriyz
 */
public class Jackson2 {

    private static final Logger LOG = LoggerFactory.getLogger(Jackson2.class);

    private Jackson2() {
    }

    /**
     * Lazy initialization of jackson mapper via static holder
     */
    private static class JacksonMapperHolder {
        private static final ObjectMapper MAPPER = jsonMapper();

        public static ObjectMapper jsonMapper() {
            final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

            final ObjectMapper mapper = new ObjectMapper();
            final DeserializationConfig deserializationConfig = mapper.getDeserializationConfig().with(jackson);
            final SerializationConfig serializationConfig = mapper.getSerializationConfig().with(jackson);
            if (deserializationConfig != null && serializationConfig != null) {
                // do nothing for now
            }
            return mapper;
        }
    }

    public static ObjectMapper createJsonMapper() {
        return JacksonMapperHolder.MAPPER;
    }

    public static String asJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(p_object);
    }

    public static String asJsonSilently(Object p_object) {
        try {
            final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
            return mapper.writeValueAsString(p_object);
        } catch (Exception e) {
            LOG.error("Failed to serialize object into json.", e);
            return "";
        }
    }

    public static org.codehaus.jackson.JsonNode asOldNode(JsonNode node) throws IOException {
        return CoreUtils.createJsonMapper().readTree(Jackson2.asJsonSilently(node));
    }
}
