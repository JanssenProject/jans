package io.jans.scim.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ProtectionServiceSelector {

    @Inject
    private Logger log;

    @Inject
    private BeanManager beanManager;

    @Inject
    private Instance<JansRestService> jansRestServiceInstance;

    private Map<String, Class<JansRestService>> mapping;

    public JansRestService select(String path) {

        return mapping.keySet().stream().filter(path::startsWith).findFirst()
            .map(mapping::get).map(jansRestServiceInstance::select)
            .map(Instance::get).orElse(null);              
        
    }
    /**
     * Builds a map around url patterns and service beans that are aimed to perform
     * actual protection
     */
    @SuppressWarnings("unchecked")
    @PostConstruct
    private void init() {
        
        mapping = new HashMap<>();
        Set<Bean<?>> beans = beanManager.getBeans(JansRestService.class, Any.Literal.INSTANCE);

        for (Bean bean : beans) {
            Class beanClass = bean.getBeanClass();

            Optional.ofNullable(beanClass.getAnnotation(BindingUrls.class))
                .map(BindingUrls.class::cast).map(BindingUrls::value)
                .map(Arrays::asList).orElse(Collections.emptyList())
                .forEach(pattern -> {
                    if (pattern.length() > 0) {
                        mapping.put(pattern, beanClass);
                    }
                });

        }

    }
    
}
