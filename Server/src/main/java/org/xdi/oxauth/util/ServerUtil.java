/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.AppInitializer;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.util.ArrayHelper;
import org.xdi.util.Util;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/12/2012
 */

public class ServerUtil {

    private final static Log LOG = Logging.getLog(ServerUtil.class);

    private ServerUtil() {
    }

    public static String asJsonSilently(Object p_object) {
        try {
            return asJson(p_object);
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
            return "";
        }
    }

    public static String asPrettyJson(Object p_object) throws IOException {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p_object);
    }

    public static String asJson(Object p_object) throws IOException {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
        return mapper.writeValueAsString(p_object);
    }

    public static CacheControl cacheControl(boolean p_noStore) {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoStore(p_noStore);
        return cacheControl;
    }

    public static CacheControl cacheControl(boolean p_noStore, boolean p_noTransform) {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoStore(p_noStore);
        cacheControl.setNoTransform(p_noTransform);
        return cacheControl;
    }


    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = new AnnotationIntrospector.Pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().withAnnotationIntrospector(pair);
        mapper.getSerializationConfig().withAnnotationIntrospector(pair);
        return mapper;
    }

    public static ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapper().configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
    }

    public static ObjectMapper jsonMapperWithUnwrapRoot() {
        return createJsonMapper().configure(DeserializationConfig.Feature.UNWRAP_ROOT_VALUE, true);
    }

    public static <T> T instance(Class p_clazz) {
        return (T) Component.getInstance(p_clazz);
    }

    public static <T> T instance(String p_name) {
        return (T) Component.getInstance(p_name);
    }

    public static LdapEntryManager getLdapManager() {
        return instance(AppInitializer.LDAP_ENTRY_MANAGER_NAME);
    }

    public static CustomAttribute getAttributeByName(List<CustomAttribute> p_list, String p_attributeName) {
        if (p_list != null && !p_list.isEmpty() && StringUtils.isNotEmpty(p_attributeName)) {
            for (CustomAttribute attr : p_list) {
                if (p_attributeName.equals(attr.getName())) {
                    return attr;
                }
            }
        }
        return null;
    }

    public static String getAttributeValueByName(List<CustomAttribute> p_list, String p_attributeName) {
        final CustomAttribute attr = getAttributeByName(p_list, p_attributeName);
        if (attr != null) {
            return attr.getValue();
        }
        return "";
    }

    public static String urlDecode(String p_str) {
        if (StringUtils.isNotBlank(p_str)) {
            try {
                return URLDecoder.decode(p_str, Util.UTF8);
            } catch (UnsupportedEncodingException e) {
                LOG.trace(e.getMessage(), e);
            }
        }
        return p_str;
    }

    public static ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable p_r) {
                Thread thread = new Thread(p_r);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public static UmaPermission convert(ResourceSetPermission p_permission, ScopeService p_umaScopeService) {
        if (p_permission != null) {
            final UmaPermission result = new UmaPermission();
            result.setResourceSetId(p_permission.getResourceSetId());
            result.setScopes(p_umaScopeService.getScopeUrlsByDns(p_permission.getScopeDns()));
            result.setExpiresAt(p_permission.getExpirationDate());
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
     * @param httpRequest -interface to provide request information for HTTP servlets.
     * @return IP address of client
     * @see <a href="http://stackoverflow.com/a/21884642/5202500">Getting IP address of client</a>
     */
    public static String getIpAddress(HttpServletRequest httpRequest) {
        final String[] HEADERS_TO_TRY = {
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
        if (request == null || !(request instanceof HttpServletRequest))
            return null;
        return (HttpServletRequest) request;
    }

    public static String getMACAddressOrNull() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (UnknownHostException e) {
            return null;
        } catch (SocketException e) {
            return null;
        }
    }

}
