/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.server.uma.service.UmaScopeService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.ArrayHelper;
import io.jans.util.Util;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.CacheControl;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 26/12/2012
 */

public class ServerUtil {

    private static final Logger log = LoggerFactory.getLogger(ServerUtil.class);
    private static final String[] HEADERS_TO_TRY = new String[]{
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private ServerUtil() {
    }

    public static GregorianCalendar now() {
        return new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    }

    public static int nowAsSeconds() {
        return (int) (new Date().getTime() / 1000L);
    }

    public static int calculateTtl(Integer expirationDateAsSeconds) {
        if (expirationDateAsSeconds == null) {
            return 0;
        }
        return expirationDateAsSeconds - nowAsSeconds();
    }

    public static int calculateTtl(Date creationDate, Date expirationDate) {
        if (creationDate != null && expirationDate != null) {
            return (int) ((expirationDate.getTime() - creationDate.getTime()) / 1000L);
        }
        return 0;
    }

    public static String asJsonSilently(Object obj) {
        try {
            return asJson(obj);
        } catch (IOException e) {
            log.trace(e.getMessage(), e);
            return "";
        }
    }

    public static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        };
    }

    public static String asPrettyJson(Object obj) throws IOException {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    public static String asJson(Object obj) throws IOException {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(obj);
    }

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

    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = AnnotationIntrospector.pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);
        return mapper;
    }

    public static ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

    public static ObjectMapper jsonMapperWithUnwrapRoot() {
        return createJsonMapper().configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    }

    public static String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

    public static PersistenceEntryManager getLdapManager() {
        return CdiUtil.bean(PersistenceEntryManager.class, ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
    }

    public static CustomAttribute getAttributeByName(List<CustomAttribute> list, String attributeName) {
        if (list != null && !list.isEmpty() && StringUtils.isNotEmpty(attributeName)) {
            for (CustomAttribute attr : list) {
                if (attributeName.equals(attr.getName())) {
                    return attr;
                }
            }
        }
        return null;
    }

    public static String getAttributeValueByName(List<CustomAttribute> list, String attributeName) {
        final CustomAttribute attr = getAttributeByName(list, attributeName);
        if (attr != null) {
            return attr.getValue();
        }
        return "";
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

    public static ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    public static io.jans.as.model.uma.UmaPermission convert(UmaPermission permission, UmaScopeService umaScopeService) {
        if (permission != null) {
            final io.jans.as.model.uma.UmaPermission result = new io.jans.as.model.uma.UmaPermission();
            result.setResourceId(permission.getResourceId());
            result.setScopes(umaScopeService.getScopeIdsByDns(permission.getScopeDns()));
            result.setExpiresAt(dateToSeconds(permission.getExpirationDate()));
            return result;
        }
        return null;
    }

    public static String getFirstValue(Map<String, String[]> map, String key) {
        if (map.containsKey(key)) {
            String[] values = map.get(key);
            if (ArrayHelper.isNotEmpty(values)) {
                return values[0];
            }
        }

        return null;
    }

    /**
     * @param httpRequest interface to provide request information for HTTP servlets.
     * @return IP address of client
     * @see <a href="http://stackoverflow.com/a/21884642/5202500">Getting IP address of client</a>
     */
    public static String getIpAddress(HttpServletRequest httpRequest) {
        for (String header : HEADERS_TO_TRY) {
            String ip = httpRequest.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return httpRequest.getRemoteAddr();
    }

    /**
     * Safe retrieves http request from FacesContext
     *
     * @return http
     */
    public static HttpServletRequest getRequestOrNull() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null)
            return null;

        ExternalContext externalContext = facesContext.getExternalContext();
        if (externalContext == null)
            return null;
        Object request = externalContext.getRequest();
        if (!(request instanceof HttpServletRequest))
            return null;
        return (HttpServletRequest) request;
    }

    public static boolean isSameRequestPath(String url1, String url2) throws MalformedURLException {
        if (StringUtils.isBlank(url1) || StringUtils.isBlank(url2)) {
            return false;
        }

        URL parsedUrl1 = new URL(url1);
        URL parsedUrl2 = new URL(url2);

        return parsedUrl1.getPath().endsWith(parsedUrl2.getPath());
    }

    public static Integer dateToSeconds(Date date) {
        return date != null ? (int) (date.getTime() / 1000) : null;
    }
}
