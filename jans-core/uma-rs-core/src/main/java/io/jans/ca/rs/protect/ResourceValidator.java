package io.jans.ca.rs.protect;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ResourceValidator {

    private ResourceValidator() {
    }

    public static boolean isHttpMethodUniqueInPath(List<RsResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return true;
        }

        Map<String, List<String>> pathToHttpMethod = Maps.newHashMap();
        for (RsResource resource : resources) {
            if (!pathToHttpMethod.containsKey(resource.getPath())) {
                pathToHttpMethod.put(resource.getPath(), Lists.<String>newArrayList());
            }

            final List<String> httpMethods = pathToHttpMethod.get(resource.getPath());
            if (resource.getConditions() != null) {
                for (Condition condition : resource.getConditions()) {

                    if (condition.getHttpMethods() != null) {
                        for (String httpMethod : condition.getHttpMethods()) {
                            if (httpMethods.contains(httpMethod)) {
                                return false;
                            } else {
                                httpMethods.add(httpMethod);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
