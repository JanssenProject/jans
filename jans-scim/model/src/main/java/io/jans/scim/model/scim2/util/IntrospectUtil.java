/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.annotations.StoreReference;
import io.jans.scim.model.scim2.annotations.Validator;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.provider.resourcetypes.ResourceType;
import io.jans.scim.model.scim2.provider.config.ServiceProviderConfig;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.provider.schema.SchemaResource;
import io.jans.scim.model.scim2.user.UserResource;

import javax.lang.model.type.NullType;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Provides miscelaneous routines to query classes/objects properties using reflection mechanisms. Additionally, this
 * class exposes some static members that contain useful information about SCIM resources that is collected upon class
 * loading.
 */
/*
 * Created by jgomer on 2017-09-14.
 */
public class IntrospectUtil {

    private static Logger log = LogManager.getLogger(IntrospectUtil.class);

    private IntrospectUtil() {}

    /**
     * This method will find a java Field with a particular name. If needed, this method will search through super classes.
     * The field does not need to be public.
     * Adapted from https://github.com/pingidentity/scim2/blob/master/scim2-sdk-common/src/main/java/com/unboundid/scim2/common/utils/SchemaUtils.java
     *
     * @param cls the java Class to search.
     * @param fieldName the name of the field to find.
     * @return A Field object, or null if no field was found
     */
    private static Field findField(final Class<?> cls, final String fieldName){

        Class<?> currentClass = cls;

        while(currentClass != null){
            Field fields[] = currentClass.getDeclaredFields();
            for (Field field : fields){
                if(field.getName().equals(fieldName))
                    return field;
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;

    }

    /**
     * Inspects a class to search for a field that corresponds to the path passed using dot notation. Every piece of the
     * path (separated by the a dot '.') is expected to have a field with the same name in the class inspected. When such
     * a field is found, the remainder of the path is processed using the class associated to the field, until the path is
     * fully consumed.
     * <p>This method starts from an initial class and visits ascendingly the class hierarchy with a route determined
     * by the components found in the path parameter.</p>
     * @param initcls Class to start the search from
     * @param path A string denoting a path to a target attribute. Examples of valid paths can be: displayName, name.givenName,
     *            addresses.locality
     * @return A Field that represents the terminal portion of the path, for instance "locality" field for "addresses.locality".
     * If no such field is found (because at some point, there was no route to go), null is returned.
     */
    public static Field findFieldFromPath(Class<?> initcls, String path){

        Class cls=initcls;
        Field f=null;

        for (String prop : path.split("\\.")) {
            f=findField(cls, prop);

            if (f!=null) {
                cls = f.getType();
                if (isCollection(cls)) {
                    Attribute attrAnnot = f.getAnnotation(Attribute.class);
                    if (attrAnnot != null)
                        cls = attrAnnot.multiValueClass();
                }
            }
            else
                break;
        }
        return f;

    }

    /**
     * Takes an opaque object and casts it to a {@literal Map<String, Object>}. Call this only if you are sure the
     * object being passed is actually an instance of {@literal Map<String, Object>}.
     * @param obj Object to be cast
     * @return A map instance
     */
    public static Map<String, Object> strObjMap(Object obj){
        return (Map<String, Object>) obj;
    }

    /**
     * Searches for a <code>Field</code> that corresponds to the path passed (using {@link #findFieldFromPath(Class, String)
     * findFieldFromPath}) and tries to find an annotation attached to such field that matches the <code>annotationClass</code>
     * parameter.
     * @param path A string denoting a path to a target attribute. Examples of valid paths can be: displayName, name.givenName,
     *            addresses.locality when {@link UserResource} class is used for <code>resourceClass</code>.
     * @param resourceClass Class to start the search from
     * @param annotationClass Class annotation to be inspected for the field
     * @param <T> Type parameter for <code>annotationClass</code>
     * @return An object of type <code>T</code>, or null if no annotation was found or the field itself couldn't be found
     */
    public static <T extends Annotation> T getFieldAnnotation(String path, Class resourceClass, Class<T> annotationClass){
        Field f=findFieldFromPath(resourceClass, path);
        return f==null ? null : f.getAnnotation(annotationClass);
    }

    /**
     * Traverses the contents of a SCIM resource and applies a set of getter methods to collect a list of values. For example,
     * if passing a {@link UserResource} object and list of getters such as <code>[getAdresses(), getStreetAddress()]</code>,
     * it will return a list with all "street addresses" that can be found inside user object.
     * @param bean A SCIM resource object
     * @param getters A list of getters methods
     * @return List of values. They are collected by scanning the getter list from beginning to end. If no values could
     * be collected at all, an empty list is returned
     */
    public static List<Object> getAttributeValues(BaseScimResource bean, final List<Method> getters){

        final List<Object> results=new ArrayList<>();

        class traversalClass{

            void traverse(Object value, int index){

                try {
                    if (value!=null && index < getters.size()) {
                        if (IntrospectUtil.isCollection(value.getClass())) {

                            Collection collection=(Collection)value;
                            if (collection.isEmpty())
                                traverse(null, index);    //stops branching...
                            else {
                                for (Object val : collection)
                                    traverse(val, index);
                            }
                        }
                        else {
                            Object val=getters.get(index).invoke(value);
                            traverse(val, index+1);
                        }
                    }
                    //Add result only if we are at the deepest level (tree tip)
                    if (index==getters.size())
                        results.add(value);
                }
                catch (Exception e){
                    log.error(e.getMessage(), e);
                }
            }

        }

        new traversalClass().traverse(bean, 0);
        return results;

    }

    /**
     * Inspects a class that represents a Java Bean and tries to find the setter <code>Method</code> associated to the
     * class field whose name is passed as parameter.
     * @param fieldName The name of the field whose setter needs to be found
     * @param clazz The Class to introspect
     * @return A Method object, null if the lookup is not successful
     * @throws Exception Upon introspection error
     */
    public static Method getSetter(String fieldName, Class clazz) throws Exception{
        PropertyDescriptor[] props = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        for (PropertyDescriptor p : props)
            if (p.getName().equals(fieldName))
                return p.getWriteMethod();
        return null;
    }

    /**
     * Inspects a class that represents a Java Bean and tries to find the getter <code>Method</code> associated to the
     * class field whose name is passed as parameter.
     * @param fieldName The name of the field whose getter needs to be found
     * @param clazz The Class to introspect
     * @return A Method object, null if the lookup is not successful
     * @throws Exception Upon introspection error
     */
    public static Method getGetter(String fieldName, Class clazz) throws Exception{
        PropertyDescriptor[] props = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        for (PropertyDescriptor p : props)
            if (p.getName().equals(fieldName))
                return p.getReadMethod();
        return null;
    }

    /**
     * Determines if the class passed implements the <code>Collection</code> interface.
     * @param clazz Class to analyze
     * @return true if the class is a Collection, false otherwise.
     */
    public static boolean isCollection(Class clazz){
        return Collection.class.isAssignableFrom(clazz);
    }

    /**
     * Returns a list with the names of the attributes that belong to an extension, so that every name is prefixed with
     * the <code>urn</code> of the extension, like this: <code>urn:attribute_name</code>
     * @param extension An {@link Extension} object
     * @return A list of prefixed names
     */
    public static List<String> getPathsInExtension(Extension extension){

        List<String> list=new ArrayList<>();
        for (String attr : extension.getFields().keySet())
            list.add(extension.getUrn() + "." + attr);
        return list;

    }

    private static List<String> requiredAttrsNames;
    private static List<String> defaultAttrsNames;
    private static List<String> alwaysAttrsNames;
    private static List<String> neverAttrsNames;
    private static List<String> requestAttrsNames;
    private static List<String> validableAttrsNames;
    private static List<String> canonicalizedAttrsNames;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when its returnability is "default"
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> defaultCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when its returnability is "request"
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> requestCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when its returnability is "always"
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> alwaysCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when its returnability is "never"
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> neverCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when the attribute is annotated as
     * "required" in the resource
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> requiredCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when the attribute is annotated with
     * some {@link Validator validation}
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> validableCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that provides access to the sequence of getter methods that allow to get the actual
     * value(s) for every possible attribute (or sub-atribute) of a SCIM resource when the attribute has
     * {@link Attribute#canonicalValues() canonical values} associated
     */
    public static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> canonicalCoreAttrs;

    /**
     * An <b>unmodifiable</b> map that stores for every possible subclass of {@link BaseScimResource BaseScimResource} a
     * <code>SortedSet</code> with the paths that lead to every single attribute/subattribute part of that resource.
     * <p>As an example, a set for the {@link UserResource} includes elements such as <code>schemas, id, name,
     * name.givenName, emails, emails.value, emails.type, ...</code></p>
     */
    public static Map<Class <? extends BaseScimResource>, SortedSet<String>> allAttrs;

    /**
     * An <b>unmodifiable</b> map that stores for every possible subclass of {@link BaseScimResource BaseScimResource} a
     * <code>Map</code> that stores paths (as stored in {@link #allAttrs}) vs. LDAP attribute names corresponding to such
     * paths.
     * <p>As an example, for {@link UserResource} this map will contain pairs like <code>(userName, uid); (displayName, displayName);
     * (meta.created, jsCreationTimestamp); (addresses, excludeAddres); (addresses.value, excludeAddres)...</code></p>
     */
    public static Map<Class <? extends BaseScimResource>, Map<String, String>> storeRefs;

    private static Map<Class<? extends BaseScimResource>, Map<String, List<Method>>> newEmptyMap(){
        return new HashMap<>();
    }

    private static void resetAttrNames(){
        requiredAttrsNames=new ArrayList<>();
        defaultAttrsNames=new ArrayList<>();
        validableAttrsNames=new ArrayList<>();
        canonicalizedAttrsNames=new ArrayList<>();
        alwaysAttrsNames=new ArrayList<>();
        neverAttrsNames=new ArrayList<>();
        requestAttrsNames=new ArrayList<>();
    }

    private static void resetMaps(){
        requiredCoreAttrs=newEmptyMap();
        defaultCoreAttrs=newEmptyMap();
        alwaysCoreAttrs=newEmptyMap();
        neverCoreAttrs=newEmptyMap();
        requestCoreAttrs=newEmptyMap();
        validableCoreAttrs=newEmptyMap();
        canonicalCoreAttrs=newEmptyMap();

        allAttrs=new HashMap<>();
        storeRefs=new HashMap<>();
    }

    private static void freezeMaps(){
        requiredCoreAttrs=Collections.unmodifiableMap(requiredCoreAttrs);
        defaultCoreAttrs=Collections.unmodifiableMap(defaultCoreAttrs);
        alwaysCoreAttrs=Collections.unmodifiableMap(alwaysCoreAttrs);
        neverCoreAttrs=Collections.unmodifiableMap(neverCoreAttrs);
        requestCoreAttrs=Collections.unmodifiableMap(requestCoreAttrs);
        validableCoreAttrs=Collections.unmodifiableMap(validableCoreAttrs);
        canonicalCoreAttrs=Collections.unmodifiableMap(canonicalCoreAttrs);

        allAttrs=Collections.unmodifiableMap(allAttrs);
        storeRefs=Collections.unmodifiableMap(storeRefs);
    }

    private static void traverseClassForNames(Class clazz, String prefix, List<Field> extraFields, boolean prune) throws Exception{

        List<Field> fields=new ArrayList<>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        fields.addAll(extraFields);

        for (Field f : fields){
            Attribute attrAnnot=f.getAnnotation(Attribute.class);
            if (attrAnnot!=null){

                String name=f.getName();
                if (prefix.length()>0)
                    name=prefix + "." + name;

                switch (attrAnnot.returned()){
                    case ALWAYS:
                        alwaysAttrsNames.add(name);
                        break;
                    case DEFAULT:
                        defaultAttrsNames.add(name);
                        break;
                    case NEVER:
                        neverAttrsNames.add(name);
                        break;
                    case REQUEST:
                        requestAttrsNames.add(name);
                        break;
                }

                if (attrAnnot.isRequired())
                    requiredAttrsNames.add(name);

                if (attrAnnot.canonicalValues().length>0)
                    canonicalizedAttrsNames.add(name);

                Validator vAnnot=f.getAnnotation(Validator.class);
                if (vAnnot!=null)
                    validableAttrsNames.add(name);

                if (!prune && attrAnnot.type().equals(AttributeDefinition.Type.COMPLEX)) {
                    Class cls = attrAnnot.multiValueClass();  //Use <T> parameter of Collection if present
                    if (cls.equals(NullType.class)) {
                        cls = f.getType();
                    }
                    if (clazz.equals(cls)) {
                        //Prevent infinite loop
                        prune = true;
                    }

                    traverseClassForNames(cls, name, new ArrayList<>(), prune);
                }
            }
        }
    }

    private static Map<String, List<Method>> computeGettersMap(List<String> attrNames, Class baseClass) throws Exception{

        Map<String, List<Method>> map=new HashMap<>();

        for (String attrName : attrNames) {
            List<Method> list =new ArrayList<>();
            Class clazz=baseClass;

            for (String prop : attrName.split("\\.")) {
                Method method=getGetter(prop, clazz);
                list.add(method);

                if (isCollection(method.getReturnType())) {  //Use class of parameter in collection
                    Field f=findField(clazz, prop);
                    Attribute attrAnnot=f.getAnnotation(Attribute.class);
                    if (attrAnnot!=null)
                        clazz=attrAnnot.multiValueClass();
                }
                else
                    clazz=method.getReturnType();
            }
            map.put(attrName, list);
        }
        return map;

    }

    static {
        try {
            List<Field> basicFields=Arrays.asList(BaseScimResource.class.getDeclaredFields());
            resetMaps();

            List<Class<? extends BaseScimResource>> resourceClasses=Arrays.asList(UserResource.class, GroupResource.class,
                    FidoDeviceResource.class, Fido2DeviceResource.class, ServiceProviderConfig.class, ResourceType.class, SchemaResource.class);

            //Perform initializations needed for all resource types
            for (Class<? extends BaseScimResource> aClass : resourceClasses){
                resetAttrNames();

                traverseClassForNames(aClass, "", basicFields, false);
                requiredCoreAttrs.put(aClass, computeGettersMap(requiredAttrsNames, aClass));
                defaultCoreAttrs.put(aClass, computeGettersMap(defaultAttrsNames, aClass));
                alwaysCoreAttrs.put(aClass, computeGettersMap(alwaysAttrsNames, aClass));
                neverCoreAttrs.put(aClass, computeGettersMap(neverAttrsNames, aClass));
                requestCoreAttrs.put(aClass, computeGettersMap(requestAttrsNames, aClass));
                validableCoreAttrs.put(aClass, computeGettersMap(validableAttrsNames, aClass));
                canonicalCoreAttrs.put(aClass, computeGettersMap(canonicalizedAttrsNames, aClass));

                allAttrs.put(aClass, new TreeSet<>());
                allAttrs.get(aClass).addAll(alwaysAttrsNames);
                allAttrs.get(aClass).addAll(defaultAttrsNames);
                allAttrs.get(aClass).addAll(neverAttrsNames);
                allAttrs.get(aClass).addAll(requestAttrsNames);
            }

            for (Class<? extends BaseScimResource> cls : resourceClasses) {
                //This is a map from attributes to storage references (e.g. LDAP attributes)
                Map<String, String> map = new HashMap<>();

                for (String attrib : allAttrs.get(cls)) {
                    Field field = findFieldFromPath(cls, attrib);
                    if (field != null) {
                        StoreReference annotation = field.getAnnotation(StoreReference.class);
                        if (annotation != null) {
                            if (StringUtils.isNotEmpty(annotation.ref()))
                                map.put(attrib, annotation.ref());
                            else {
                                List<Class<? extends BaseScimResource>> clsList = Arrays.asList(annotation.resourceType());
                                int i = clsList.indexOf(cls);
                                if (i>=0 && i<annotation.refs().length)
                                    map.put(attrib, annotation.refs()[i]);
                            }
                        }
                    }
                }
                storeRefs.put(cls, map);
            }
            //Make them all unmodifiable
            freezeMaps();
        }
        catch (Exception e){
            log.fatal(e.getMessage(), e);
        }
    }

}