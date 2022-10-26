package io.jans.scim.service.scim2.serialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.scim2.ExtensionService;

import org.slf4j.Logger;

/**
 * Created by jgomer on 2017-10-01.
 */
@ApplicationScoped
public class ScimResourceSerializer {

    @Inject
    private Logger log;

    @Inject
    private ExtensionService extService;

    private ObjectMapper mapper = new ObjectMapper();

    private Set<String> expandAttributesPaths(String attributes, String defaultSchemaUrn, List<String> schemas, SortedSet<String> attribs) {

        Set<String> set = new HashSet<>();

        for (String attr : attributes.split(",")) {
            String shorterName = attr.replaceAll("\\s", "");
            set.add(ScimResourceUtil.adjustNotationInPath(shorterName, defaultSchemaUrn, schemas));
        }

        Set<String> enlargedSet = new HashSet<>();

        //attribs is already sorted
        for (String basicAttr : set) {
            enlargedSet.add(basicAttr);

            for (String elem : attribs.tailSet(basicAttr + ".")) {
                if (elem.startsWith(basicAttr + ".")) {
                    enlargedSet.add(elem);
                } else {
                    break;
                }
            }
        }
        return enlargedSet;

    }

    private void buildIncludeSet(SortedSet<String> include, Class<? extends BaseScimResource> resourceClass,
                                 List<String> schemas, String attributes, String exclussions) {

        Set<String> tempSet;
        Set<String> alwaysSet = IntrospectUtil.alwaysCoreAttrs.get(resourceClass).keySet();
        Set<String> neverSet = IntrospectUtil.neverCoreAttrs.get(resourceClass).keySet();
        Set<String> defaultSet = new HashSet<>();

        //Here we assume all attributes part of extensions have returnability="default"...
        SortedSet<String> extendedSet = new TreeSet<>();
        for (Extension ext : extService.getResourceExtensions(resourceClass)) {
            extendedSet.add(ext.getUrn());
            extendedSet.addAll(IntrospectUtil.getPathsInExtension(ext));
        }

        defaultSet.addAll(IntrospectUtil.defaultCoreAttrs.get(resourceClass).keySet());
        defaultSet.addAll(extendedSet);

        String defaultSchema = ScimResourceUtil.getDefaultSchemaUrn(resourceClass);

        if (attributes != null) {
            log.info("buildIncludeSet. Processing attributes query param (excludedAttributes ignored)");

            extendedSet.addAll(IntrospectUtil.allAttrs.get(resourceClass));
            tempSet = expandAttributesPaths(attributes, defaultSchema, schemas, extendedSet);
            tempSet.removeAll(neverSet);
            include.addAll(tempSet);

        } else if (exclussions != null) {

            log.info("buildIncludeSet. Processing excludedAttributes query param");
            extendedSet.addAll(IntrospectUtil.allAttrs.get(resourceClass));
            tempSet = defaultSet;
            tempSet.removeAll(expandAttributesPaths(exclussions, defaultSchema, schemas, extendedSet));
            include.addAll(tempSet);

        } else {
            log.info("buildIncludeSet. No attributes neither excludedAttributes query param were passed");
            include.addAll(defaultSet);
        }
        include.addAll(alwaysSet);

    }

    private boolean containsProperty(SortedSet<String> properties, String prefix, String key) {

        key = key.startsWith("$") ? key.substring(1) : key;     //makes attributes like $ref to be accepted...
        String property = (prefix.length() == 0) ? key : prefix + "." + key;
        Set<String> set = properties.tailSet(property);

        boolean flag = set.contains(property);
        if (!flag) {
            for (String prop : set) {
                if (prop.startsWith(property + ".")) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;

    }

    private String getNewPrefix(String prefix, String key) {
        return prefix + (prefix.length() == 0 ? "" : ".") + key;
    }

    private Map<String, Object> smallerMap(String prefix, Map<String, Object> value, SortedSet<String> include) {
        LinkedHashMap<String, Object> smallMap = new LinkedHashMap<>();
        traverse(prefix, value, smallMap, include);
        return smallMap.size() == 0 ? null : smallMap;
    }

    /**
     * Section 2.5 of RFC 7643: When a resource is expressed in JSON format, unassigned attributes, although they are
     * defined in schema, MAY be omitted for compactness
     * @param prefix
     * @param map
     * @param destination
     * @param include
     */
    private void traverse(String prefix, Map<String, Object> map, LinkedHashMap<String, Object> destination, SortedSet<String> include) {

        for (String key : map.keySet()) {
            Object value = map.get(key);

            if (value != null && containsProperty(include, prefix, key)) {

                if (value instanceof Map) {
                    value = smallerMap(getNewPrefix(prefix, key), IntrospectUtil.strObjMap(value), include);
                } else if (IntrospectUtil.isCollection(value.getClass())) {
                    List list = new ArrayList();
                    Map<String, Object> innerMap;

                    for (Object item : (Collection) value) {
                        if (item != null) {
                            if (item instanceof Map) {
                                innerMap = smallerMap(getNewPrefix(prefix, key), IntrospectUtil.strObjMap(item), include);
                                if (innerMap != null)
                                    list.add(innerMap);
                            } else {
                                list.add(item);
                            }
                        }
                    }
                    value = list;
                }
                if (value != null) {
                    destination.put(key, value);
                }
            }
        }

    }

    public String serialize(BaseScimResource resource, String attributes, String exclusions) throws Exception {

        SortedSet<String> include = new TreeSet<>();
        Class<? extends BaseScimResource> resourceClass = resource.getClass();
        buildIncludeSet(include, resourceClass, new ArrayList<>(resource.getSchemas()), attributes, exclusions);
        log.trace("serialize. Attributes to include: {}", include);

        //Do generic serialization. This works for any POJO (not only subclasses of BaseScimResource)
        Map<String, Object> map = mapper.convertValue(resource, new TypeReference<Map<String, Object>>() {});
        //Using LinkedHashMap allows recursive routines to visit submaps in the same order as fields appear in java classes
        LinkedHashMap<String, Object> newMap = new LinkedHashMap<>();
        traverse("", map, newMap, include);

        String result = mapper.writeValueAsString(newMap);
        log.trace("serialize. Output is {}", result);

        return result;
    }

    public String serialize(BaseScimResource resource) throws Exception {
        return serialize(resource, null, null);
    }

    public ObjectMapper getListResponseMapper() {

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("ListResponseModule", Version.unknownVersion());
        module.addSerializer(ListResponse.class, new ListResponseJsonSerializer(this));
        mapper.registerModule(module);

        return mapper;
    }

}
