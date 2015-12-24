package org.xdi.oxd.rs.protect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.xdi.oxd.common.CoreUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
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
            resourceMap.put(resource.getId(), resource);
        }
    }

    public static RsProtector instance(InputStream inputStream) throws IOException {
        try {
            final RsResourceList resourceList = CoreUtils.createJsonMapper().readValue(inputStream, RsResourceList.class);
            return new RsProtector(resourceList.getResources());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public boolean hasAccess(String id, String... scope) {
        return hasAccess(id, Arrays.asList(scope));
    }

    public boolean hasAccess(String id, Collection<String> scope) {
        Preconditions.checkNotNull(scope);
        final RsResource rsResource = resourceMap.get(id);
        if (rsResource != null) {
            final List<String> scopes = rsResource.getScopes();
            if (scopes != null) {
                return scopes.containsAll(scope);
            }
        }
        return false;
    }

}
