/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2;

import static io.jans.scim.model.scim2.Constants.USER_EXT_SCHEMA_DESCRIPTION;
import static io.jans.scim.model.scim2.Constants.USER_EXT_SCHEMA_ID;
import static io.jans.scim.model.scim2.Constants.USER_EXT_SCHEMA_NAME;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;

import io.jans.model.GluuAttribute;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.extensions.ExtensionField;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.service.AttributeService;
import io.jans.model.attribute.AttributeDataType;

import org.slf4j.Logger;

@ApplicationScoped
public class ExtensionService {

    @Inject
    private Logger log;

    @Inject
    AppConfiguration appConfiguration;

    @Inject
    private AttributeService attributeService;

    public List<Extension> getResourceExtensions(Class<? extends BaseScimResource> cls) {

        List<Extension> list = new ArrayList<>();
        try {
            // Currently support one extension only for User Resource
            if (cls.equals(UserResource.class)) {

                Map<String, ExtensionField> fields = new HashMap<>();

                for (GluuAttribute attribute : attributeService.getSCIMRelatedAttributes()) {
                    if (Optional.ofNullable(attribute.getScimCustomAttr()).orElse(false)) {
                        // first non-null check is needed because certain entries do not have the multivalue attribute set

                        ExtensionField field = new ExtensionField();
                        field.setDescription(attribute.getDescription());
                        field.setType(attribute.getDataType());
                        field.setMultiValued(Optional.ofNullable(attribute.getOxMultiValuedAttribute()).orElse(false));
                        field.setName(attribute.getName());

                        fields.put(attribute.getName(), field);
                    }
                }

                String uri = appConfiguration.getUserExtensionSchemaURI();
                if (StringUtils.isEmpty(uri)) {
                    uri = USER_EXT_SCHEMA_ID;
                }
                Extension ext = new Extension(uri);
                ext.setFields(fields);

                if (uri.equals(USER_EXT_SCHEMA_ID)) {
                    ext.setName(USER_EXT_SCHEMA_NAME);
                    ext.setDescription(USER_EXT_SCHEMA_DESCRIPTION);
                }

                list.add(ext);
            }
        } catch (Exception e) {
            log.error("An error ocurred when building extension for {}", cls.getName());
            log.error(e.getMessage(), e);
        }
        return list;

    }

    public List<String> getUrnsOfExtensions(Class<? extends BaseScimResource> cls) {

        List<String> list = new ArrayList<>();
        for (Extension ext : getResourceExtensions(cls))
            list.add(ext.getUrn());

        return list;

    }

    /**
     * Transforms the value passed in case it is of type DATE. Depending on the param <code>ldapBackend</code>, the
     * value will be a generalized time string, otherwise an ISO-like date with no offset or time zone
     * @param field The extension field associated to the value passed
     * @param val The value of the field
     * @param ldapBackend Whether the backend DB is LDAP or not
     * @return Value transformed (kept as is if unrelated to DATEs)
     */
    public Object getAttributeValue(ExtensionField field, Object val, boolean ldapBackend) {

        AttributeDataType type = field.getType();
        Object value = val;
        if (type.equals(AttributeDataType.DATE)) {
            //If the date object is passed directly to the persistence layer, it fails for both backend types
            value = ldapBackend ? DateUtil.ISOToGeneralizedStringDate(val.toString())
                    : DateUtil.gluuCouchbaseISODate(val.toString());
        }
        return value;

    }

    public List<Object> getAttributeValues(ExtensionField field, Collection valuesHolder, boolean ldapBackend) {

        List<Object> values = new ArrayList<>();
        for (Object elem : valuesHolder) {
            // Despite valuesHolder is not null, it can be a collection with null elements...
            if (elem != null) {
                values.add(getAttributeValue(field, elem, ldapBackend));
            }
        }
        values.remove(Collections.singletonList(null));
        return values;

    }

    /**
     * Builds a list of objects based on the supplied String values passed and the
     * extension field passed. The strings are converted according to the type
     * asociated to the field: for STRING the value is left as is; for DATE the
     * value is converted to a String following the ISO date format; for NUMERIC an
     * Integer/Double is created from the value supplied.
     * @param ldapBackend Whether the underlying database is an ldap directory
     * @param field
     *            An ExtensionField
     * @param strValues
     *            A non-empty String array with the values associated to the field
     *            passed. These values are coming from LDAP
     * @return List of opaque values
     */
    public List<Object> convertValues(ExtensionField field, String strValues[], boolean ldapBackend) {

        List<Object> values = new ArrayList<>();

        for (String val : strValues) {
            // In practice, there should not be nulls in strValues
            if (val != null) {
                Object value;

                //See io.jans.scim.model.scim2.util.DateUtil.gluuCouchbaseISODate()
                if (!ldapBackend && field.getType().equals(AttributeDataType.DATE)) {
                    try {
                        DateTimeFormatter.ISO_DATE_TIME.parse(val);
                        value = val;
                    } catch (Exception e) {
                        value = null;
                    }
                } else {
                    value = ExtensionField.valueFromString(field, val);
                }
                // won't happen either (value being null) because calls to this method occurs
                // after lots of validations have taken place
                if (value != null) {
                    values.add(value);
                    log.debug("convertValues. Added value '{}'", value.toString());
                }
            }
        }
        return values;

    }

    public Extension extensionOfAttribute(Class<? extends BaseScimResource> cls, String attribute) {

        List<Extension> extensions = getResourceExtensions(cls);
        Extension belong = null;

        try {
            for (Extension ext : extensions) {
                if (attribute.startsWith(ext.getUrn() + ":")) {
                    attribute = attribute.substring(ext.getUrn().length() + 1);

                    for (String fieldName : ext.getFields().keySet())
                        if (attribute.equals(fieldName)) {
                            belong = ext;
                            break;
                        }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return belong;

    }

    public ExtensionField getFieldOfExtendedAttribute(Class<? extends BaseScimResource> cls, String attribute) {

        List<Extension> extensions = getResourceExtensions(cls);
        ExtensionField field = null;

        try {
            for (Extension ext : extensions) {
                if (attribute.startsWith(ext.getUrn() + ":")) {
                    attribute = attribute.substring(ext.getUrn().length() + 1);

                    for (ExtensionField f : ext.getFields().values())
                        if (attribute.equals(f.getName())) {
                            field = f;
                            break;
                        }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return field;
    }

}
