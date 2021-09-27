/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.util;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.extensions.ExtensionField;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.management.InvalidAttributeValueException;

import static io.jans.scim.model.scim2.AttributeDefinition.Mutability.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Helper class to traverse a SCIM resource object recursively.
 */
/*
 * Created by jgomer on 2017-09-25.
 */
class traversalClass {

    private Logger log = LogManager.getLogger(getClass());

    String error;
    private Class base;

    traversalClass(Class baseClass){
        base=baseClass;
    }

    private String getNewPrefix(String prefix, String key){
        return prefix + (prefix.length()==0 ? "" : ".") + key;
    }

    private Map<String, Object> smallerMap(String prefix, Map<String, Object> source, Object destination, boolean replacing){
        Map<String, Object> smallMap = (destination==null) ? new HashMap<>() : IntrospectUtil.strObjMap(destination);
        traverse(prefix, source, smallMap, replacing);
        return smallMap.isEmpty() ? null : smallMap;
    }

    void traverse(String prefix, Map<String, Object> source, Map<String, Object> destination, boolean replacing){

        for (String key : source.keySet()) {
            Object value = source.get(key);
            Object destValue = destination.get(key);

            if (value!=null && error==null) {
                //Atributes related to extensions evaluate null here
                Attribute attrAnnot=IntrospectUtil.getFieldAnnotation(getNewPrefix(prefix, key), base, Attribute.class);

                if (attrAnnot != null && !attrAnnot.mutability().equals(READ_ONLY)) {
                    if (value instanceof Map) {
                        value = smallerMap(getNewPrefix(prefix, key), IntrospectUtil.strObjMap(value), destValue, replacing);
                    } else if (attrAnnot.mutability().equals(IMMUTABLE) && destValue != null && !value.equals(destValue)) {
                        //provokes no more traversals
                        error = "Invalid value passed for immutable attribute " + key;
                        value = null;
                    }

                    if (value != null) {

                        if (IntrospectUtil.isCollection(value.getClass())) {
                            Collection col=(Collection) value;
                            int size=col.size();

                            if (!replacing) {    //we need to add to the existing collection
                                if (destValue!=null) {

                                    if (IntrospectUtil.isCollection(destValue.getClass()))
                                        col.addAll((Collection) destValue);
                                    else
                                        log.warn("Value {} was expected to be a collection", destValue);
                                }
                            }
                            //Do the arrangement so that only one primary="true" can stay in data
                            value = col.isEmpty() ? null : adjustPrimarySubAttributes(col, size);
                        }
                        destination.put(key, value);
                    }
                }
            }
        }
    }

    void traverseDelete(Map<String, Object> source, String path){

        int i=path.indexOf(".");
        Object value=null;

        if (i==-1)
            source.remove(path);
        else {
            String key = path.substring(0, i);
            value = source.get(key);
            path=path.substring(i+1);
        }

        if (value!=null)
            try {
                //If it's a map we must recurse
                traverseDelete(IntrospectUtil.strObjMap(value), path);
            }
            catch (Exception e){
                if (IntrospectUtil.isCollection(value.getClass())){
                    Collection col=(Collection) value;
                    for (Object item : col) {
                        if (item instanceof Map)
                            traverseDelete(IntrospectUtil.strObjMap(item), path);
                    }
                }
            }

    }

    private Collection adjustPrimarySubAttributes(Collection input, int nFreshEntries){

        int i;
        Object array[]=input.toArray();
        for (i=0; i<nFreshEntries; i++){
            Object item=array[i];
            if (item!=null && item instanceof Map){
                Map<String, Object> map = IntrospectUtil.strObjMap(item);
                Object primaryObj=map.get("primary");
                if (primaryObj!=null && primaryObj.toString().equals("true"))
                    break;
            }
            else    //Means this collection is not made up of complex attributes, so we can abort the operation
                i=array.length;
        }
        //Set the remaining to primary="false"
        for (i=i+1;i<array.length;i++){
            Object item=array[i];
            if (item!=null && item instanceof Map) {
                Map<String, Object> map = IntrospectUtil.strObjMap(item);
                Object primaryObj = map.get("primary");
                if (primaryObj != null && primaryObj.toString().equals("true")){
                    map.put("primary", false);
                    log.info("Setting primary = false for item whose associated value is {}", map.get("value"));
                }
            }
        }
        return Arrays.asList(array);
    }

}

/**
 * This class contains methods to facilitate transformation, and manipulation of data inside SCIM resource objects, as
 * well as some miscellaneous routines.
 */
/*
 * Created by jgomer on 2017-09-25.
 */
public class ScimResourceUtil {

    private static Logger log = LogManager.getLogger(ScimResourceUtil.class);

    private static ObjectMapper mapper=new ObjectMapper();

    private ScimResourceUtil() {}

    private static void attachExtensionInfo(Map<String, Object> source, Map<String, Object> destination, List<Extension> extensions, boolean replacing){

        log.debug("attachExtensionInfo");

        for (Extension extension : extensions){
            String urn=extension.getUrn();
            Object extendedAttrsObj=source.get(urn);

            if (extendedAttrsObj!=null){

                Map<String, Object> extendedAttrs=IntrospectUtil.strObjMap(extendedAttrsObj);
                Map<String, ExtensionField> fields=extension.getFields();

                Map<String, Object> destMap = destination.get(urn)==null ? new HashMap<>() : IntrospectUtil.strObjMap(destination.get(urn));

                for (String attr : fields.keySet()){
                    Object value=extendedAttrs.get(attr);

                    if (value!=null) {

                        if (IntrospectUtil.isCollection(value.getClass())) {
                            Collection col = (Collection) value;

                            if (!replacing){
                                Object destValue=destMap.get(attr);
                                if (destValue != null) {

                                    if (!IntrospectUtil.isCollection(destValue.getClass()))
                                        log.warn("Value {} was expected to be a collection", destValue);
                                    else
                                        col.addAll((Collection) destMap.get(attr));
                                }
                            }
                            value = col.size()==0 ? null : col;
                        }
                        destMap.put(attr, value);
                    }
                }
                destination.put(urn, destMap);
            }
        }

    }

    private static void deleteCustomAttribute(Map<String, Object> source, String path, List<Extension> extensions){

        //All custom attributes are non-complex so we must search for the last dot
        int i=path.lastIndexOf(".");
        if (i==-1)
            log.warn("Path not recognized {}", path);
        else {
            String key = path.substring(i+1);
            path=path.substring(0,i);

            for (Extension ext : extensions)
                if (ext.getUrn().equals(path)){
                    Map<String, Object> submap=IntrospectUtil.strObjMap(source.get(path));
                    //get method above could have returned null (no extended attributes for source object)
                    if (submap != null) {
                        submap.put(key, null);
                    }
                }

        }
    }

    private static BaseScimResource transferToResource(BaseScimResource origin, final BaseScimResource destination,
                                                      List<Extension> extensions, boolean replacing) throws InvalidAttributeValueException{

        log.debug("transferToResource. Processing {} operation", replacing ? "replace" : "add");

        Map<String, Object> fromMap = mapper.convertValue(origin, new TypeReference<Map<String,Object>>(){});
        Map<String, Object> toMap = mapper.convertValue(destination, new TypeReference<Map<String,Object>>(){});

        log.debug("transferToResource. Recursive traversal of resource is taking place");
        traversalClass tclass=new traversalClass(origin.getClass());
        tclass.traverse("", fromMap, toMap, replacing);
        attachExtensionInfo(fromMap, toMap, extensions, replacing);

        if (tclass.error==null)
            return mapper.convertValue(toMap, origin.getClass());
        else
            throw new InvalidAttributeValueException(tclass.error);

    }

    /**
     * Returns an object which is the result of incorporating the information found in the <code>replacementDataSource</code>
     * parameter to the information existing in <code>originalDataSource</code> object by doing replacements.
     * The transference of data follows these rules:
     * <ul>
     * <li>Ignores changes in readonly attributes</li>
     * <li>Ignores null values (for single-valued attributes) in <code>replacementDataSource</code></li>
     * <li>Nullifies multi-valued attributes when empty array is passed in <code>replacementDataSource</code></li>
     * <li>Immutable attributes must match in both input objects or else exception is thrown. However, if the value in
     * <code>originalDataSource</code> is missing (null), the value coming from <code>replacementDataSource</code> is kept</li>
     * <li>When a multi-valued attribute is passed in <code>replacementDataSource</code>, no existing data in the
     * <code>originalDataSource</code> is retained, that is, the replacement is not partial but thorough: it's not an
     * item-by-item replacement</li>
     * </ul>
     * @param replacementDataSource Object with the information to be incorporated. Only non-null attributes of this
     *                              object end up being transfered to the result
     * @param originalDataSource Object (SCIM resource) that provides the original data
     * @param extensions A list of <code>Extensions</code> associated to parameter <code>originalDataSource</code>.
     *                   This helps to manipulate the transference of custom attributes values.
     * @return A new object that contains the result of data transference. Neither <code>replacementDataSource</code>
     * nor <code>originalDataSource</code> are changed after a call to this method
     * @throws InvalidAttributeValueException When recursive traversal of <code>replacementDataSource</code> fails or
     * if the rule of immutable attribute was not fulfilled
     */
    public static BaseScimResource transferToResourceReplace(BaseScimResource replacementDataSource, BaseScimResource originalDataSource,
                                                            List<Extension> extensions) throws InvalidAttributeValueException{
        //This method is suitable for the replace operation via PUT, or for a PATCH with op="replace" and no "value selection" filter
        return transferToResource(replacementDataSource, originalDataSource, extensions, true);
    }

    /**
     * This method applies the same copying rules of {@link #transferToResourceReplace(BaseScimResource, BaseScimResource, List)
     * transferToResourceReplace} except for the following:
     * <ul>
     * <li>When a multi-valued attribute is passed in <code>replacementDataSource</code>, the existing data in the
     * <code>originalDataSource</code> object is retained, and the items in the former object are prepended to the
     * existing collection.</li>
     * </ul>
     * @param replacementDataSource Object with the information to be incorporated. Only non-null attributes of this
     *                                object end up being transferred to the result
     * @param originalDataSource Object (SCIM resource) that provides the original data
     * @param extensions A list of <code>Extensions</code> associated to parameter <code>originalDataSource</code>.
     *                   This helps to manipulate the transference of custom attributes values.
     * @return A new object that contains the result of data transference. Neither <code>replacementDataSource</code>
     * nor <code>originalDataSource</code> are changed after a call to this method
     * @throws InvalidAttributeValueException When recursive traversal of <code>replacementDataSource</code> fails or
     * if the rule of immutable attribute was not fulfilled
     */
    public static BaseScimResource transferToResourceAdd(BaseScimResource replacementDataSource, BaseScimResource originalDataSource,
                                                            List<Extension> extensions) throws InvalidAttributeValueException{
        return transferToResource(replacementDataSource, originalDataSource, extensions, false);
    }

    /**
     * Returns a SCIM resource with the same data found in <code>origin</code> object, except for the attribute referenced
     * by <code>path</code> being removed from the output. In other words, this method nullifies an attribute.
     * @param origin The resource having the the original data
     * @param path An attribute path (in dot notation). Examples could be: <code>displayName, emails.type, addresses,
     *             meta.lastModified</code>.
     * @param extensions A list of <code>Extension</code>s associated to <code>origin</code> Object
     * @return The resulting object: data in origin without the attribute referenced by <code>path</code>
     * @throws InvalidAttributeValueException If there is an attempt to remove an attribute annotated as {@link Attribute#isRequired()
     * required} or {@link io.jans.scim.model.scim2.AttributeDefinition.Mutability#READ_ONLY read-only}
     */
    public static BaseScimResource deleteFromResource(BaseScimResource origin, String path, List<Extension> extensions)
            throws InvalidAttributeValueException {

        Field f=IntrospectUtil.findFieldFromPath(origin.getClass(), path);
        if (f!=null){
            Attribute attrAnnot = f.getAnnotation(Attribute.class);
            if (attrAnnot != null && (attrAnnot.mutability().equals(READ_ONLY) || attrAnnot.isRequired()))
                throw new InvalidAttributeValueException("Cannot remove read-only or required attribute " + path);
        }

        Map<String, Object> map = mapper.convertValue(origin, new TypeReference<Map<String,Object>>(){});
        traversalClass tclass=new traversalClass(origin.getClass());

        if (f==null)    //Extensions stuff
            deleteCustomAttribute(map, path, extensions);
        else
            tclass.traverseDelete(map, path);

        return mapper.convertValue(map, origin.getClass());

    }

    /**
     * Returns the <code>Schema</code> annotation found in the class passed as parameter.
     * @param cls A class representing a SCIM resource
     * @return The annotation found or null if there is no such
     */
    public static Schema getSchemaAnnotation(Class<? extends BaseScimResource> cls){
        return cls.getAnnotation(Schema.class);
    }

    /**
     * Returns the <code>urn</code> associated to the default schema of the SCIM resource whose class is passed as parameter.
     * @param cls A class representing a SCIM resource
     * @return The urn (obtained by calling {@link Schema#id()} on the appropriate <code>Schema</code> annotation) or null
     * if there is no such annotation in the class <code>cls</code>
     */
    public static String getDefaultSchemaUrn(Class<? extends BaseScimResource> cls){
        Schema schema=getSchemaAnnotation(cls);
        return schema==null ? null : schema.id();
    }

    /**
     * Removes from an attribute path the schema <code>urn</code> that might prefix such path. The <code>urn</code> to
     * remove will correspond to the default schema urn of a SCIM resource type whose class is passed as parameter.
     * @param cls A class that represents a SCIM resource type
     * @param attribute An attribute path (potentially prefixed by a <code>urn</code>)
     * @return The attribute with no prefix. As an example, <code>attribute_name</code> is returned if
     * <code>urn:attribute_name</code> is the value of attribute parameter (as long as urn represent the default urn for
     * this resource)
     */
    public static String stripDefaultSchema(Class<? extends BaseScimResource> cls, String attribute){

        String val=attribute;
        String defaultSchema=getDefaultSchemaUrn(cls);
        if (StringUtils.isNotEmpty(attribute) && StringUtils.isNotEmpty(defaultSchema)) {
            if (attribute.startsWith(defaultSchema + ":"))
                val = attribute.substring(defaultSchema.length() +1);
        }
        return val;

    }

    /**
     * Returns the (human-readable) type of a SCIM resource based on its class. In practice this will be something like
     * "User" or "Group". The type is obtained by calling {@link Schema#name()} of the respective class annotation.
     * @param cls A class that represents a SCIM resource type
     * @return A string with the proper value, or null if there is no {@link Schema} annotation found
     */
    public static String getType(Class<? extends BaseScimResource> cls){
        Schema annot=ScimResourceUtil.getSchemaAnnotation(cls);
        return annot==null ? null : annot.name();
    }

    public static String adjustNotationInPath(String path, String defaultUrn, List<String> schemas){

        for (String urn : schemas){
            if (path.startsWith(urn + ":")) {
                if (urn.equals(defaultUrn))
                    path = path.substring(urn.length()+1);
                else
                    path = path.substring(0, urn.length()) + "." + path.substring(urn.length()+1);
            }
        }
        return path;

    }

    public static String[] splitPath(String path, List<String> urns){

        String prefix="";
        for (String urn : urns)
            if (path.startsWith(urn)){
                prefix=urn;
                break;
            }

        if (prefix.length()>0) {

            List<String> pieces=new ArrayList<>();
            pieces.add(prefix);

            path=path.substring(prefix.length());

            if (path.length()>0) {
                String subpath=path.substring(path.startsWith(".") ? 1 : 0);
                pieces.addAll(Arrays.asList(subpath.split("\\.")));
            }
            return pieces.toArray(new String[]{});
        }
        else
            return path.split("\\.");

    }

    /**
     * Takes a SCIM resource and "fixes" inconsistencies in "primary" subattribute: in a multivalued attribute setting,
     * only one of the items in the collection can have <i><code>"primary" : true</code></i>. Thus, for every collection
     * involved (e.g. addresses, emails... in {@link io.jans.scim.model.scim2.user.UserResource}), it switches all
     * occurrences where "primary" is currently <code>true</code> to <code>false</code>, except for the first one found.
     * @param resource SCIM resource object
     */
    public static void adjustPrimarySubAttributes(BaseScimResource resource){

        String fragment=".primary";
        Class<? extends BaseScimResource> cls=resource.getClass();

        //parents will contain the parent path (and associated getter list) where there are primary subattrs, e.g. emails,
        //ims... if we are talking about users
        List<String> parents=new ArrayList<>();
        for (String path : IntrospectUtil.allAttrs.get(cls))
            if (path.endsWith(fragment))
                parents.add(path.substring(0, path.length() - fragment.length()));

        List<Map<String,List<Method>>> niceList=Arrays.asList(IntrospectUtil.defaultCoreAttrs.get(cls),
                IntrospectUtil.neverCoreAttrs.get(cls), IntrospectUtil.alwaysCoreAttrs.get(cls));

        log.info("adjustPrimarySubAttributes. Revising \"primary\":true uniqueness constraints");
        for (String path : parents){
            //Searh path in the maps
            for (Map<String,List<Method>> niceMap : niceList) {
                try {
                    if (niceMap.containsKey(path)) {
                        //Here we will get a singleton list that contains the multivalued complex objects (or an empty one at least)
                        List<Object> list = IntrospectUtil.getAttributeValues(resource, niceMap.get(path));
                        if (list.size()>0){
                            list=(List<Object>) list.get(0);

                            if (list!=null && list.size()>1) {  //Ensure is not empty or singleton
                                Class clz = list.get(0).getClass();     //All items are supposed to belong to the same class
                                //Find getter and setter of "primary" property
                                Method setter = IntrospectUtil.getSetter(fragment.substring(1), clz);
                                Method getter = IntrospectUtil.getGetter(fragment.substring(1), clz);
                                int trues = 0;

                                for (Object item : list) {
                                    Object primaryVal = getter.invoke(item);
                                    trues += primaryVal != null && primaryVal.toString().equals("true") ? 1 : 0;
                                    if (trues > 1) {  //Revert to false
                                        setter.invoke(item, false);
                                        log.info("adjustPrimarySubAttributes. Setting primary = false for an item (a previous one was already primary = true)");
                                    }
                                }
                            }
                        }
                        break;  //skip the rest of nicemaps
                    }
                }
                catch (Exception e){
                    log.error(e.getMessage(), e);
                }
            }
        }

    }

    public static BaseScimResource clone(BaseScimResource object) {
        Map<String, Object> map = mapper.convertValue(object, new TypeReference<Map<String,Object>>(){});
        return mapper.convertValue(map, object.getClass());
    }

}
