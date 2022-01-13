/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.annotations;

import io.jans.scim.model.scim2.AttributeDefinition;

import javax.lang.model.type.NullType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to attach meta information to attributes and subattributes of SCIM resources (descendants of class
 * {@link io.jans.scim.model.scim2.BaseScimResource BaseScimResource}).
 * <p>This annotation reflects what is shown in section 2.2 RFC 7643. Default values assigned per section 7.</p>
 */
/*
 * Created by jgomer on 2017-09-04.
 * Originally based on https://github.com/pingidentity/scim2/blob/master/scim2-sdk-common/src/main/java/com/unboundid/scim2/common/annotations/Attribute.java
 * CAUTION: If you change default values here, you must search all usages of this annotation and edit classes accordingly
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Attribute {

    /**
     * Determines if the attribute value is case sensitive.
     * @return A flag indicating the attribute value's case sensitivity.
     */
    boolean isCaseExact() default false;

    /**
     * Determines if the attribute value is required.
     * @return A flag indicating if the attribute value is required.
     */
    boolean isRequired() default false;

    /**
     * The description of the attribute.
     * @return The description of the attribute.
     */
    String description() default "";

    /**
     * The canonical values that may appear in an attribute.
     * @return The canonical values that may appear in an attribute as an Array of Strings.
     */
    String[] canonicalValues() default {};

    /**
     * The return constraint for the attribute.
     * @return The return constraint for the attribute.
     */
    AttributeDefinition.Returned returned() default AttributeDefinition.Returned.DEFAULT;

    /**
     * The uniqueness constraint for the attribute.
     * @return The uniqueness constraint for the attribute.
     */
    AttributeDefinition.Uniqueness uniqueness() default AttributeDefinition.Uniqueness.NONE;

    /**
     * The reference types for the attribute.
     * @return The reference types for the attribute as an Array of Strings.
     */
    String[] referenceTypes() default {};

    /**
     * The mutability constraint for the attribute.
     *
     * @return The mutability constraint for the attribute.
     */
    AttributeDefinition.Mutability mutability() default AttributeDefinition.Mutability.READ_WRITE;

    /**
     * If the attribute is multi-valued, this holds the type of the child object.
     * @return For a multi-valued attribute, the type of the child object.
     */
    Class multiValueClass() default NullType.class;

    /**
     * The type of the attribute according to types defined by section 2.3 of RFC 7643
     * @return Associated data type
     */
    AttributeDefinition.Type type() default AttributeDefinition.Type.STRING;

}
