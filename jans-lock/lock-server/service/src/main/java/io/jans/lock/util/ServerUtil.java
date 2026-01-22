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

import io.grpc.Context;
import io.grpc.Metadata;
import io.jans.util.Util;
import jakarta.ws.rs.core.CacheControl;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.1, 10/07/2024
 */

public class ServerUtil {

	private static final Logger log = LoggerFactory.getLogger(ServerUtil.class);

	public static final String PRAGMA = "Pragma";
	public static final String NO_CACHE = "no-cache";

	private static final String UNKNOWN = "unknown";

    public static final Context.Key<String> CLIENT_IP_CONTEXT_KEY = Context.key("client-ip");
    private static final Metadata.Key<String> X_FORWARDED_FOR = Metadata.Key.of("x-forwarded-for", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_REAL_IP = Metadata.Key.of("x-real-ip", Metadata.ASCII_STRING_MARSHALLER);

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

    /**
     * Get client IP address from current context
     * @return client IP address or "unknown" if not determined
     */
	public static String getClientContextIpAddress() {
		String clientIp = CLIENT_IP_CONTEXT_KEY.get(Context.current());

		if (clientIp == null) {
			return UNKNOWN;
		}

		return clientIp;
	}

    /**
     * Set client IP address from current context
     * @return 
     */
    public static Context setClientContextIpAddress(String clientIp) {
    	return Context.current().withValue(CLIENT_IP_CONTEXT_KEY, clientIp);
    }

    /**
     * Get client IP address when ServerCall is available (e.g., in interceptor)
     */
    public static String getGrpcClientIpAddress(io.grpc.ServerCall<?, ?> call, Metadata headers) {
        try {
            // Method 1: Try to get from metadata headers
            String ipFromHeaders = getIpFromHeaders(headers);
            if (ipFromHeaders != null) {
                log.debug("Client IP from headers: {}", ipFromHeaders);
                return ipFromHeaders;
            }

            // Method 2: Get from ServerCall attributes
            if (call != null) {
                io.grpc.Attributes attributes = call.getAttributes();
                java.net.SocketAddress remoteAddr = attributes.get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                if (remoteAddr instanceof java.net.InetSocketAddress) {
                    java.net.InetSocketAddress inetAddr = (java.net.InetSocketAddress) remoteAddr;
                    String ip = inetAddr.getAddress().getHostAddress();
                    log.debug("Client IP from ServerCall attributes: {}", ip);
                    return ip;
                }
            }

            log.warn("Could not determine client IP address");
            return UNKNOWN;
        } catch (Exception e) {
            log.error("Error getting client IP address", e);
            return UNKNOWN;
        }
    }

    /**
     * Extract IP directly from headers (for use in interceptor)
     */
    public static String getIpFromHeaders(Metadata headers) {
        if (headers == null) {
            return null;
        }
        
        try {          
            // 1. Try X-Forwarded-For
            String xForwardedFor = headers.get(X_FORWARDED_FOR);
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                return ips[0].trim();
            }
            
            // 2. Try X-Real-IP
            String xRealIp = headers.get(X_REAL_IP);
            if (xRealIp != null && !xRealIp.trim().isEmpty()) {
                return xRealIp.trim();
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Error extracting IP from headers", e);
            return null;
        }
    }

}