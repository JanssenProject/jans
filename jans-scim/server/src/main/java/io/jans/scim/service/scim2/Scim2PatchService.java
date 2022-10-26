/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2;

import static io.jans.scim.model.scim2.AttributeDefinition.Mutability.IMMUTABLE;
import static io.jans.scim.model.scim2.AttributeDefinition.Mutability.READ_ONLY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.lang.model.type.NullType;
import javax.management.InvalidAttributeValueException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang.StringUtils;

import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchOperationType;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.antlr.scimFilter.ScimFilterParserService;
import io.jans.scim.service.antlr.scimFilter.util.FilterUtil;
import io.jans.util.Pair;

import org.slf4j.Logger;

@ApplicationScoped
public class Scim2PatchService {

    @Inject
    private Logger log;

    @Inject
    private ScimFilterParserService filterService;

    @Inject
    private ExtensionService extService;

    public BaseScimResource applyPatchOperation(BaseScimResource resource, PatchOperation operation)
            throws Exception {
        return applyPatchOperation(resource, operation, filter -> false);
    }

    public BaseScimResource applyPatchOperation(BaseScimResource resource, PatchOperation operation,    
            Predicate<String> selectionFilterSkipPredicate) throws Exception {

        BaseScimResource result = null;
        Map<String, Object> genericMap = null;
        PatchOperationType opType = operation.getType();
        Class<? extends BaseScimResource> clazz = resource.getClass();
        String path = operation.getPath();

        log.debug("applyPatchOperation of type {}", opType);

        //Determine if operation is with value filter
        if (StringUtils.isNotEmpty(path) && !operation.getType().equals(PatchOperationType.ADD)) {
            Pair<Boolean, String> pair = validateBracketedPath(path);

            if (pair.getFirst()) {
                String valSelFilter = pair.getSecond();
                if (valSelFilter == null) {
                    throw new SCIMException("Unexpected syntax in value selection filter");
                } else {
                    int i = path.indexOf("[");
                    String attribute = path.substring(0, i);

                    i = path.lastIndexOf("].");
                    String subAttribute = i == -1 ? "" : path.substring(i + 2);

                    //Abort earlier
                    if (selectionFilterSkipPredicate.test(valSelFilter)) {
                        log.info("Patch operation will be skipped");
                        return resource;
                        
                    } else {
                        return applyPatchOperationWithValueFilter(resource, operation, 
                                valSelFilter, attribute, subAttribute);
                    }
                }
            }
        }

        if (!opType.equals(PatchOperationType.REMOVE)) {
            Object value = operation.getValue();
            List<String> extensionUrns = extService.getUrnsOfExtensions(clazz);

            if (value instanceof Map) {
                genericMap = IntrospectUtil.strObjMap(value);
            } else {
                //It's an atomic value or an array
                if (StringUtils.isEmpty(path)) {
                    throw new SCIMException("Value(s) supplied for resource not parseable");
                }

                //Create a simple map and trim the last part of path
                String subPaths[] = ScimResourceUtil.splitPath(path, extensionUrns);
                genericMap = Collections.singletonMap(subPaths[subPaths.length - 1], value);

                if (subPaths.length == 1) {
                    path = "";
                } else {
                    path = path.substring(0, path.lastIndexOf("."));
                }
            }

            if (StringUtils.isNotEmpty(path)) {
                //Visit backwards creating a composite map
                String subPaths[] = ScimResourceUtil.splitPath(path, extensionUrns);
                for (int i = subPaths.length - 1; i >= 0; i--) {

                    //Create a string consisting of all subpaths until the i-th
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j <= i; j++) {
                        sb.append(subPaths[j]).append(".");
                    }

                    Attribute annot = IntrospectUtil.getFieldAnnotation(sb.substring(0, sb.length() - 1), clazz, Attribute.class);
                    boolean multivalued = !(annot == null || annot.multiValueClass().equals(NullType.class));

                    Map<String, Object> genericBiggerMap = new HashMap<>();
                    genericBiggerMap.put(subPaths[i], multivalued ? Collections.singletonList(genericMap) : genericMap);
                    genericMap = genericBiggerMap;
                }
            }

            log.debug("applyPatchOperation. Generating a ScimResource from generic map: {}", genericMap.toString());
        }

        //Try parse genericMap as an instance of the resource
        ObjectMapper mapper = new ObjectMapper();
        BaseScimResource alter = opType.equals(PatchOperationType.REMOVE) ? resource : mapper.convertValue(genericMap, clazz);
        List<Extension> extensions = extService.getResourceExtensions(clazz);

        switch (operation.getType()) {
            case REPLACE:
                result = ScimResourceUtil.transferToResourceReplace(alter, resource, extensions);
                break;
            case ADD:
                result = ScimResourceUtil.transferToResourceAdd(alter, resource, extensions);
                break;
            case REMOVE:
                result = ScimResourceUtil.deleteFromResource(alter, operation.getPath(), extensions);
                break;
        }
        return result;

    }

    private BaseScimResource applyPatchOperationWithValueFilter(BaseScimResource resource, PatchOperation operation,
                                                                String valSelFilter, String attribute, String subAttribute)
            throws SCIMException, InvalidAttributeValueException {

        String path = operation.getPath();
        ObjectMapper mapper = new ObjectMapper();
        Class<? extends BaseScimResource> cls = resource.getClass();
        Map<String, Object> resourceAsMap = mapper.convertValue(resource, new TypeReference<Map<String, Object>>() { });
        List<Map<String, Object>> list;

        Attribute attrAnnot = IntrospectUtil.getFieldAnnotation(attribute, cls, Attribute.class);
        if (attrAnnot != null) {
            if (!attrAnnot.multiValueClass().equals(NullType.class) && attrAnnot.type().equals(AttributeDefinition.Type.COMPLEX)) {
                Object colObject = resourceAsMap.get(attribute);
                list = colObject == null ? null : new ArrayList<>((Collection<Map<String, Object>>) colObject);
            } else {
                throw new SCIMException(String.format("Attribute '%s' expected to be complex multi-valued", attribute));
            }
        } else {
            throw new SCIMException(String.format("Attribute '%s' not recognized or expected to be complex multi-valued", attribute));
        }

        if (list == null) {
            log.info("applyPatchOperationWithValueFilter. List of values for {} is empty. Operation has no effect", attribute);
        } else {
            try {
                valSelFilter = FilterUtil.preprocess(valSelFilter, cls);
                ParseTree parseTree = filterService.getParseTree(valSelFilter);

                List<Integer> matchingIndexes = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    if (filterService.complexAttributeMatch(parseTree, list.get(i), attribute, cls)) {
                        matchingIndexes.add(0, i);  //Important: add so that resulting list is reverse-ordered
                    }
                }

                if (subAttribute.length() > 0 && matchingIndexes.size() > 0 && operation.getType().equals(PatchOperationType.REMOVE)) {
                    //per spec (section 3.5.2.2 RFC 7644) subAttribute must not be required or read-only
                    Attribute subAttrAnnot = IntrospectUtil.getFieldAnnotation(attribute + "." + subAttribute, cls, Attribute.class);

                    if (subAttrAnnot != null && (subAttrAnnot.mutability().equals(READ_ONLY) || subAttrAnnot.isRequired())) {
                        throw new InvalidAttributeValueException("Cannot remove read-only or required attribute " + attribute + "." + subAttribute);
                    }
                }
                /*
                Here we differ from spec (see section 3.5.2.3/4 of RFC7644. If no record match is made, we are supposed to
                return error 400 with scimType of noTarget. But this is clearly inconvenient
                */
                log.info("There are {} entries matching the filter '{}'", matchingIndexes.size(), path);

                for (Integer index : matchingIndexes) {
                    if (operation.getType().equals(PatchOperationType.REMOVE)) {
                        if (subAttribute.length() == 0) {   //Remove the whole item
                            list.remove(index.intValue());      //If intValue is not used, the remove(Object) method is called!
                        } else {    //remove subattribute only
                            list.get(index).remove(subAttribute);
                        }
                    } else {
                        applyPartialUpdate(attribute, subAttribute, list, index, operation.getValue(), cls);
                    }
                }

                log.trace("New {} list is:\n{}", attribute, mapper.writeValueAsString(list));
                resourceAsMap.put(attribute, list.isEmpty() ? null : list);
                resource = mapper.convertValue(resourceAsMap, cls);
            } catch (InvalidAttributeValueException ei) {
                throw ei;
            } catch (Exception e) {
                log.info("Error processing Patch operation with value selection path '{}'", path);
                log.error(e.getMessage(), e);
                throw new SCIMException(e.getMessage(), e);
            }
        }
        return resource;

    }

    /**
     * It tries to determine if this is a valid path in terms of PATCH operation for the case of value selection filter.
     * Example: emails[value co ".com"]
     * @param path
     * @return
     */
    private Pair<Boolean, String> validateBracketedPath(String path) {

        boolean isFilterExpression;
        String selFilter = null;

        int lBracketIndex = path.indexOf("[");
        //Check if characters preceding bracket look like attribute name
        isFilterExpression = (lBracketIndex > 0) && path.substring(0, lBracketIndex).matches("[a-zA-Z]\\w*");

        if (isFilterExpression) {
            int rBracketIndex = path.lastIndexOf("]");
            int lenm1 = path.length() - 1;
            /*
            It will be valid if character ] is the last of string, or if it is followed by a dot and at least one
            letter (thus specifying a subattribute name). Examples:
             - emails[type eq null]
             - addresses[value co "any[...]thing"]
             - ims[value eq "hi"].primary
             */
            if ((rBracketIndex > lBracketIndex) && (rBracketIndex == lenm1 ||
                    (lenm1 - rBracketIndex > 1 && path.charAt(rBracketIndex + 1) == '.' && Character.isLetter(path.charAt(rBracketIndex + 2))))) {
                selFilter = path.substring(lBracketIndex + 1, rBracketIndex);
            }
        }
        return new Pair<>(isFilterExpression, selFilter);

    }

    private void applyPartialUpdate(String attribute, String subAttribute, List<Map<String, Object>> list, int index, Object value,
                                    Class<? extends BaseScimResource> cls) throws InvalidAttributeValueException {

        if (subAttribute.length() == 0) {
            //Updates the whole item in the list after passing mutability check, see section 3.5.2 RFC 7644:
            //"Each operation against an attribute MUST be compatible with the attribute's mutability and schema ... "
            Map<String, Object> map = IntrospectUtil.strObjMap(value);

            for (String subAttr : map.keySet()) {
                assertMutability(attribute + "." + subAttr, list.get(index).get(subAttr), map.get(subAttr), cls);
            }
            list.set(index, map);
        } else {
            //Updates a subattribute only
            assertMutability(attribute + "." + subAttribute, list.get(index).get(subAttribute), value, cls);
            list.get(index).put(subAttribute, value);
        }

    }

    private void assertMutability(String path, Object currentVal, Object value, Class<? extends BaseScimResource> cls) throws InvalidAttributeValueException {
        /*
        log.debug("path: {}, currentVal null: {}, value null: {}, classes: {} and {}",
            path, currentVal == null, value == null, currentVal == null ? "": currentVal.getClass().getName(),
            value == null ? "": value.getClass().getName());
        */
        Attribute attrAnnot = IntrospectUtil.getFieldAnnotation(path, cls, Attribute.class);
        if (attrAnnot != null) {
            if (attrAnnot.mutability().equals(IMMUTABLE) && currentVal != null && !value.equals(currentVal)) {
                throw new InvalidAttributeValueException("Invalid value passed for immutable attribute " + path);
            }
        }

    }

}
