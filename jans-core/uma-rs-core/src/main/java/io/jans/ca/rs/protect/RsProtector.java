package io.jans.ca.rs.protect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/12/2015
 */

public class RsProtector {

    private Map<String, RsResource> resourceMap = Maps.newHashMap();

    public RsProtector(List<RsResource> resourceList) {
        Preconditions.checkNotNull(resourceList);

        for (RsResource resource : resourceList) {
            resourceMap.put(resource.getPath(), resource);
        }
    }

    public static RsProtector instance(InputStream inputStream) throws IOException {
        try {
            final RsResourceList resourceList = read(inputStream);
            return new RsProtector(resourceList.getResources());
        } finally {
            Closeables.closeQuietly(inputStream);
        }
    }

    public static RsResourceList read(String json) throws IOException {
        return Jackson.createJsonMapper().readValue(json, RsResourceList.class);
    }

    public static RsResourceList read(InputStream json) throws IOException {
        return Jackson.createJsonMapper().readValue(json, RsResourceList.class);
    }

    public boolean hasAccess(String path, String httpMethod, String... presentScope) {
        Preconditions.checkNotNull(presentScope);

        return hasAccess(path, httpMethod, Arrays.asList(presentScope));
    }

    public boolean hasAccess(String path, String httpMethod, List<String> presentScopes) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(presentScopes);
        Preconditions.checkNotNull(httpMethod);

        Preconditions.checkState(!presentScopes.isEmpty(), "Scopes can't be empty.");

        final RsResource rsResource = resourceMap.get(path);
        if (rsResource != null) {
            final List<String> requiredScopes = rsResource.scopes(httpMethod);
            if (requiredScopes != null) {
                return !Collections.disjoint(requiredScopes, presentScopes); // contains any
            }
        }
        return false;
    }

    public Map<String, RsResource> getResourceMap() {
        return resourceMap;
    }
}
