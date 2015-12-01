/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.ws.rs.core.CacheControl;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.AppInitializer;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.util.ArrayHelper;
import org.xdi.util.Util;

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

    public static RegisterPermissionRequest convert(ResourceSetPermission p_permission, ScopeService p_umaScopeService) {
        if (p_permission != null) {
            final RegisterPermissionRequest result = new RegisterPermissionRequest();
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

}
