package org.gluu.oxd.rs.protect.resteasy;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.gluu.oxauth.model.uma.JsonLogicNodeParser;
import org.gluu.oxauth.model.uma.UmaResource;
import org.gluu.oxauth.model.uma.UmaResourceResponse;
import org.gluu.oxd.rs.protect.Condition;
import org.gluu.oxd.rs.protect.RsProtector;
import org.gluu.oxd.rs.protect.RsResource;

import java.util.Collection;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/04/2016
 */

public class ResourceRegistrar {

    private static final Logger LOG = Logger.getLogger(ResourceRegistrar.class);

    private final Map<Key, RsResource> resourceMap = Maps.newHashMap();
    private final Map<Key, String> idMap = Maps.newHashMap();

    private final PatProvider patProvider;
    private final ServiceProvider serviceProvider;

    public ResourceRegistrar(PatProvider patProvider, ServiceProvider serviceProvider) {
        this.patProvider = patProvider;
        this.serviceProvider = serviceProvider;
    }

    public RsProtector getProtector() {
        return new RsProtector(Lists.newArrayList(resourceMap.values()));
    }

    public RsResource getRsResource(Key key) {
        return resourceMap.get(key);
    }

    public void register(Collection<RsResource> resources) {
        Preconditions.checkNotNull(resources);

        for (RsResource resource : resources) {
            register(resource);
        }
    }

    public Key getKey(String path, String httpMethod) {
        if (Strings.isNullOrEmpty(path) || Strings.isNullOrEmpty(httpMethod)) {
            return null;
        }

        String id = idMap.get(new Key(path, Lists.newArrayList(httpMethod)));
        if (id != null) {
            return new Key(path, Lists.newArrayList(httpMethod));
        }

        for (Key key : idMap.keySet()) {
            if (path.startsWith(key.getPath()) && key.getHttpMethods().contains(httpMethod)) {
                return key;
            }
        }
        return null;
    }

    public String getResourceSetId(Key key) {
        return key != null ? idMap.get(key) : null;
    }

    public String getResourceSetId(String path, String httpMethod) {
        return getResourceSetId(getKey(path, httpMethod));
    }

    private void register(RsResource rsResource) {
        try {
            for (Condition condition : rsResource.getConditions()) {
                Key key = new Key(rsResource.getPath(), condition.getHttpMethods());

                UmaResource resource = new UmaResource();
                resource.setName(key.getResourceName());

                if (condition.getScopeExpression() != null && JsonLogicNodeParser.isNodeValid(condition.getScopeExpression().toString())) {
                    resource.setScopeExpression(condition.getScopeExpression().toString());
                    resource.setScopes(JsonLogicNodeParser.parseNode(condition.getScopeExpression().toString()).getData());
                } else {
                    resource.setScopes(condition.getScopes());
                }
                //set creation and expiration timestamp
                if (isSafeToInt(rsResource.getIat())) {
                    resource.setIat(rsResource.getIat());
                }

                if (isSafeToInt(rsResource.getExp())) {
                    resource.setExp(rsResource.getExp());
                }

                UmaResourceResponse resourceResponse = serviceProvider.getResourceService().addResource("Bearer " + patProvider.getPatToken(), resource);

                Preconditions.checkNotNull(resourceResponse.getId(), "Resource ID can not be null.");

                resourceMap.put(key, rsResource);
                idMap.put(key, resourceResponse.getId());

                LOG.debug("Registered resource, path: " + key.getPath() + ", http methods: " + condition.getHttpMethods() + ", id: " + resourceResponse.getId());
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    public void putRegisteredResource(RsResource resource, String idOfResourceOnAuthorizationServer) {
        for (Condition condition : resource.getConditions()) {
            Key key = new Key(resource.getPath(), condition.getHttpMethods());

            resourceMap.put(key, resource);
            idMap.put(key, idOfResourceOnAuthorizationServer);

            LOG.debug("Put registered resource, path: " + key.getPath() + ", http methods: " + condition.getHttpMethods() + ", id: " + idOfResourceOnAuthorizationServer);
        }
    }

    public PatProvider getPatProvider() {
        return patProvider;
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public Map<Key, RsResource> getResourceMapCopy() {
        return Maps.newHashMap(resourceMap);
    }

    public Map<Key, String> getIdMapCopy() {
        return Maps.newHashMap(idMap);
    }

    public static boolean isSafeToInt(Integer input) {
        return input != null && input > 0;
    }
}
