/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.Validations;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Validator;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.extensions.ExtensionField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class provides static methods to validate whether a (SCIM) resource instance fulfills certain characteristics -
 * regarded to formatting, mutability, uniqueness, etc. This allows to adhere more closely to SCIM spec
 */
public class ResourceValidator {

    private Logger log = LogManager.getLogger(getClass());

    private static final String REQUIRED_ATTR_NOTFOUND="Required attribute %s not found";
    private static final String WRONG_SCHEMAS_ATTR="Wrong value of schemas attribute";
    private static final String UNKNOWN_EXTENSION="Extension %s not recognized";
    private static final String ATTR_NOT_RECOGNIZED="Attribute %s not part of schema %s";
    private static final String ERROR_PARSING_EXTENDED="Error parsing extended attributes";
    private static final String ATTR_VALIDATION_FAILED ="Unexpected value for attribute %s";
    private static final String WRONG_CARDINALITY="Value passed has wrong cardinality";

    private BaseScimResource resource;
    private Class<? extends BaseScimResource> resourceClass;
    private List<Extension> extensions;

    /**
     * Construct a instance of this class
     * @param resource A SCIM resource object (the target of validation)
     * @param extensions List of extensions associated to this resource
     */
    public ResourceValidator(BaseScimResource resource, List<Extension> extensions){
        this.resource=resource;
        resourceClass=resource.getClass();
        this.extensions=extensions;
    }

    /**
     * Inspects the resource passed in the constructor and determines if the attributes annotated as {@link Attribute#isRequired()
     * required} in the <code>Class</code> of the resource were all provided (not null).
     * In lax mode, if an attribute was marked as "required" and is part of a multi-valued complex attribute, no validation takes
     * place if the involved list is null or empty.
     * @param laxRequiredness True denotes lax mode, False normal validation mode (strict)
     * @throws SCIMException When a validation does not pass (there is a missing value in a required attribute)
     */
    public void validateRequiredAttributes(boolean laxRequiredness) throws SCIMException {

        Map<String, List<Method>> map= IntrospectUtil.requiredCoreAttrs.get(resourceClass);
        
        for (String attributePath : map.keySet()) {
            boolean checkValues = !laxRequiredness;
            List<Method> methods = map.get(attributePath);

            if (laxRequiredness) {
                int len = methods.size();

                if (len > 1) {
                    Method parentMethod = methods.get(len - 2);

                    if (IntrospectUtil.isCollection(parentMethod.getReturnType())) {
                        List<Object> items = IntrospectUtil.getAttributeValues(resource, 
                                methods.subList(0, len - 1));            
                        checkValues = !items.isEmpty();
                    }
                }
            }

            if (checkValues) {
                log.debug("Validating existence of required attribute '{}'", attributePath);

                for (Object val : IntrospectUtil.getAttributeValues(resource, methods)) {
                    if (val == null) {
                        log.error("Error getting value of required attribute '{}'", attributePath);
                        throw new SCIMException(String.format(REQUIRED_ATTR_NOTFOUND, attributePath));
                    }
                }
            }
        }

    }

    /**
     * Inspects the resource passed in the constructor and applies validations for every attribute annotated with
     * {@link Validator}. Validations are of different nature as seen{@link Validations here}.
     * @throws SCIMException When a validation does not pass (the {@link Validations#apply(Validations, Object) apply}
     * method returns false)
     */
    public void validateValidableAttributes() throws SCIMException {

        Map<String, List<Method>> map=IntrospectUtil.validableCoreAttrs.get(resourceClass);

        for (String attributePath : map.keySet()) {

            Field f=IntrospectUtil.findFieldFromPath(resourceClass, attributePath);
            Validations valToApply=f.getAnnotation(Validator.class).value();
            log.debug("Validating value(s) of attribute '{}'", attributePath);

            for (Object val : IntrospectUtil.getAttributeValues(resource, map.get(attributePath))) {
                if (val!=null && !Validations.apply(valToApply, val)) {
                    log.error("Error validating attribute '{}', wrong value supplied: '{}'", attributePath, val.toString());
                    throw new SCIMException(String.format(ATTR_VALIDATION_FAILED, attributePath));
                }
            }
        }

    }

    /**
     * Inspects the resource passed in the constructor and for every attribute annotated with a non-empty collection of
     * {@link Attribute#canonicalValues() canonical values}, it checks whether the attribute value matches any of the
     * canonical values supplied.
     * <p>This method should be called after a successful call to {@link #validateRequiredAttributes()}.</p>
     * @throws SCIMException When a validation does not pass (there is no match for any of the attributes inspected)
     */
    public void validateCanonicalizedAttributes() throws SCIMException{

        Map<String, List<Method>> map=IntrospectUtil.canonicalCoreAttrs.get(resourceClass);

        for (String attributePath : map.keySet()) {

            Attribute attrAnnot=IntrospectUtil.getFieldAnnotation(attributePath, resourceClass, Attribute.class);
            List<String> canonicalVals=Arrays.asList(attrAnnot.canonicalValues());
            log.debug("Validating values of canonical attribute '{}'", attributePath);

            for (Object val : IntrospectUtil.getAttributeValues(resource, map.get(attributePath))) {
                if (!canonicalVals.contains(val.toString())) {
                    log.error("Error validating canonical attribute '{}', wrong value supplied: '{}'", attributePath, val.toString());
                    throw new SCIMException(String.format(ATTR_VALIDATION_FAILED, attributePath));
                }
            }
        }
    }

    /**
     * Inspects the {@link BaseScimResource#getSchemas() schemas} attribute of the resource passed in the constructor and
     * checks the default schema <code>urn</code> associated to the resource type is present in the list. If some of the
     * <code>urn</code>s part of the <code>Extension</code>s passed in the constructor are contained in the list, the validation is also
     * successful.
     * <p>This method should be called after a successful call to {@link #validateRequiredAttributes()}.</p>
     * @throws SCIMException If there is no {@link BaseScimResource#getSchemas() schemas} in this resource or if some of
     * the <code>urn</code>s there are not known.
     */
    public void validateSchemasAttribute() throws SCIMException {

        Set<String> schemaList = new HashSet<>(resource.getSchemas());
        if (schemaList.isEmpty())
            throw new SCIMException(WRONG_SCHEMAS_ATTR);

        Set<String> allSchemas=new HashSet<>();
        allSchemas.add(ScimResourceUtil.getDefaultSchemaUrn(resourceClass));
        for (Extension ext : extensions)
            allSchemas.add(ext.getUrn());

        schemaList.removeAll(allSchemas);

        if (schemaList.size()>0)    //means that some wrong extension urn is there
            throw new SCIMException(WRONG_SCHEMAS_ATTR);

    }

    /**
     * Validates if an attribute part of an extension is consistent with an arbitrary value passed.
     * @param extension Extension where the attribute exists
     * @param attribute The name of the attribute inside the extension
     * @param value The value to be checked (never null)
     * @throws SCIMException When the value is inconsistent, or the attribute does not belong to the extension. As an
     * example, consider an attribute whose type is "NUMERIC": if the value passed was "Hi", this is clearly an error.
     */
    private void validateDataTypeExtendedAttr(Extension extension, String attribute, Object value) throws SCIMException{

        ExtensionField field=extension.getFields().get(attribute);
        if (field==null)
            throw new SCIMException(String.format(ATTR_NOT_RECOGNIZED, attribute, extension.getUrn()));
        else{
            log.debug("validateDataTypeExtendedAttr. Checking attribute '{}' for type '{}' with value '{}'", attribute, field.getType().toString(), value.toString());

            //look up if the field in this extension is consistent with the value passed
            if (ExtensionField.valueOf(field, value)==null)
                throw new SCIMException(String.format(ATTR_VALIDATION_FAILED, attribute));
        }

    }

    /**
     * Inspects the resource passed in the constructor and for every extended attribute (see {@link BaseScimResource#getCustomAttributes()},
     * the attribute's value is checked to see if it complies with the data type it is supposed to belong to. This
     * information is obtained from the list of <code>Extension</code>s passed in the constructor (every {@link ExtensionField}
     * has an associated {@link ExtensionField#getType() type}.
     * <p>When an attribute is {@link ExtensionField#isMultiValued() multi-valued}, every single item inside the collection
     * is validated.</p>
     * @throws SCIMException When any of the validations do not pass or an attribute seems not to be part of a known schema.
     */
    public void validateExtendedAttributes() throws SCIMException{

        //Note: throughout this method, we always ignore presence of nulls

        //Gets all extended attributes (see the @JsonAnySetter annotation in BaseScimResource)
        Map<String, Object> extendedAttributes=resource.getCustomAttributes();

        //Iterate over every extension of the resource object (in practice it will be just one at most)
        for (String schema : extendedAttributes.keySet()) {
            //Validate if the schema referenced in the extended attributes is contained in the valid set of extension

            Extension extension=null;
            for (Extension ext : extensions)
                if (ext.getUrn().equals(schema)) {
                    extension = ext;
                    break;
                }

            if (extension!=null) {
                log.debug("validateExtendedAttributes. Revising attributes under schema {}", schema);

                try {
                    //Obtains a generic map consisting of all name/value(s) pairs associated to this schema
                    Map<String, Object> attrsMap = IntrospectUtil.strObjMap(extendedAttributes.get(schema));

                    for (String attr : attrsMap.keySet()) {
                        Object value = attrsMap.get(attr);
                        if (value != null) {
                            /*
                             Gets the class associated to the value of current attribute. For extended attributes, we
                             should only see coming: String, Integer, Double, boolean, and Collection.
                             Different things will be rejected
                             */
                            Class cls = value.getClass();
                            boolean isCollection=IntrospectUtil.isCollection(cls);

                            //If the attribute coming is unknown, NPE will be thrown and we are covered
                            log.debug("validateExtendedAttributes. Got value(s) for attribute '{}'", attr);
                            //Check if the multivalued custom attribute is consistent with the nature of the value itself
                            if (isCollection == extension.getFields().get(attr).isMultiValued()){
                                if (isCollection) {
                                    for (Object elem : (Collection) value)
                                        if (elem!=null)
                                            validateDataTypeExtendedAttr(extension, attr, elem);
                                }
                                else
                                    validateDataTypeExtendedAttr(extension, attr, value);
                            }
                            else
                                throw new SCIMException(String.format(ATTR_VALIDATION_FAILED, attr) + 
                                    "; " + WRONG_CARDINALITY);
                        }
                    }
                } catch (SCIMException se) {
                    log.error(se.getMessage(), se);
                    throw se;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new SCIMException(ERROR_PARSING_EXTENDED);
                }
            }
            else
                throw new SCIMException(String.format(UNKNOWN_EXTENSION, schema));
        }

    }

}
