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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.AppInitializer;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.util.ArrayHelper;
import org.xdi.util.Util;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 26/12/2012
 */

public class ServerUtil {

    private final static Logger log = LoggerFactory.getLogger(ServerUtil.class);

    private ServerUtil() {
    }

    public static String asJsonSilently(Object p_object) {
        try {
            return asJson(p_object);
        } catch (IOException e) {
            log.trace(e.getMessage(), e);
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

    public static <T> T getContextBean(BeanManager beanManager, Type type, String beanName) {
		Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(type, NamedLiteral.of(beanName)));
		if (bean == null) {
			return null;
		}

		T existingInstance = beanManager.getContext(bean.getScope()).get(bean, beanManager.createCreationalContext(bean));

    	return existingInstance;
	}

    public static <T> T getContextualReference(BeanManager bm, Set<Bean<?>> beans, Class<?> type) {
		if (beans == null || beans.size() == 0) {
			return null;
		}

		// If we would resolve to multiple beans then BeanManager#resolve would throw an AmbiguousResolutionException
		Bean<?> bean = bm.resolve(beans);
		if (bean == null) {
			return null;
		} else {
			CreationalContext<?> creationalContext = bm.createCreationalContext(bean);
			return (T) bm.getReference(bean, type, creationalContext);
		}
	}

    public static <T> Instance<T> instance(Class<T> p_clazz) {
		return CDI.current().select(p_clazz);
    }    	

    public static <T> Instance<T> instance(Class<T> p_clazz, String name) {
		return CDI.current().select(p_clazz, NamedLiteral.of(name));
    }    	

    public static <T> T bean(Class<T> p_clazz) {
		return instance(p_clazz).get();
    }    	

    public static <T> T bean(Class<T> p_clazz, String name) {
		return instance(p_clazz, name).get();
    }    	

    public static <T> void destroy(Class<T> p_clazz) {
		Instance<T> instance = instance(p_clazz);
		if (instance.isResolvable()) {
			instance.destroy(instance.get());
		}
    }    	

    public static <T> T destroy(Class<T> p_clazz, String name) {
		Instance<T> instance = instance(p_clazz, name);
		if (instance.isResolvable()) {
			T obj = instance.get();
			instance.destroy(obj);
			
			return obj;
		}

		return null;
    }    	

    public static LdapEntryManager getLdapManager() {
        return bean(LdapEntryManager.class, AppInitializer.LDAP_ENTRY_MANAGER_NAME);
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
                log.trace(e.getMessage(), e);
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

    public static boolean isSameRequestPath(String url1, String url2) throws MalformedURLException {
    	if ((url1 == null) || (url2 == null)) {
    		return false;
    	}
    	
    	URL parsedUrl1 = new URL(url1);
    	URL parsedUrl2 = new URL(url2);
    	
    	return parsedUrl1.getPath().endsWith(parsedUrl2.getPath());
    }

}
