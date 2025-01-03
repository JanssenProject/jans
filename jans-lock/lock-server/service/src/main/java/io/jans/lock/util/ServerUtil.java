/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.util.Util;
import jakarta.ws.rs.core.CacheControl;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.1, 10/07/2024
 */

public class ServerUtil {

	private static final Logger log = LoggerFactory.getLogger(ServerUtil.class);

	public static final String PRAGMA = "Pragma";
	public static final String NO_CACHE = "no-cache";

	public static CacheControl cacheControl(boolean noStore) {
		final CacheControl cacheControl = new CacheControl();
		cacheControl.setNoStore(noStore);
		return cacheControl;
	}

	public static CacheControl cacheControl(boolean noStore, boolean noTransform) {
		final CacheControl cacheControl = new CacheControl();
		cacheControl.setNoStore(noStore);
		cacheControl.setNoTransform(noTransform);
		return cacheControl;
	}

	public static CacheControl cacheControlWithNoStoreTransformAndPrivate() {
		final CacheControl cacheControl = cacheControl(true, false);
		cacheControl.setPrivate(true);
		return cacheControl;
	}

    public static String asPrettyJson(Object obj) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    public static String asJson(Object obj) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(obj);
    }

    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(jackson);
        mapper.getSerializationConfig().with(jackson);
        return mapper;
    }

    public static ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

    public static ObjectMapper jsonMapperWithUnwrapRoot() {
        return createJsonMapper().configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    }

    public static String toPrettyJson(ObjectNode jsonObject) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }
    

    public static String urlDecode(String str) {
        if (StringUtils.isNotBlank(str)) {
            try {
                return URLDecoder.decode(str, Util.UTF8);
            } catch (UnsupportedEncodingException e) {
                log.trace(e.getMessage(), e);
            }
        }
        return str;
    }
}
