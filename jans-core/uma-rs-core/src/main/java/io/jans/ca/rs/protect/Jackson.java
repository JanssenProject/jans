package io.jans.ca.rs.protect;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/04/2016
 */

public class Jackson {

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

    /**
     * UTF-8 encoding string
     */
    public static final String UTF8 = "UTF-8";

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Jackson.class);

    /**
     * Converts object to json string.
     *
     * @param p_object object to convert to string
     * @return json object representation in string format
     * @throws java.io.IOException if io problems occurs
     */
    public static String asJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(p_object);
    }

    /**
     * Creates json mapper for json object serialization/deserialization.
     *
     * @return object mapper
     */
    public static ObjectMapper createJsonMapper() {
        return JacksonMapperHolder.MAPPER;
    }

    public static String asPrettyJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        final ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
        return writer.writeValueAsString(p_object);
    }

    public static String asJsonSilently(Object p_object) {
        try {
            return asPrettyJson(p_object);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return "";
        }
    }
}
