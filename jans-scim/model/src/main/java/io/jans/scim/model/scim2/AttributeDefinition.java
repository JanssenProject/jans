/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

/**
 * Represents SCIM attribute characteristics which are worth to model using Java enums: mutability, returned, type and
 * uniqueness. See section 2.2 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-04.
 * Adapted from https://github.com/pingidentity/scim2/blob/master/scim2-sdk-common/src/main/java/com/unboundid/scim2/common/types/AttributeDefinition.java
 */
public class AttributeDefinition {

    /**
     * An enumeration of the data types for attributes or subattributes.
     */
    public enum Type {

        STRING("string"),
        BOOLEAN("boolean"),
        DECIMAL("decimal"),
        INTEGER("integer"),
        DATETIME("dateTime"),
        BINARY("binary"),
        REFERENCE("reference"),
        COMPLEX("complex");

        private String name;

        /**
         * Constructs an <code>Type</code> object.
         * @param name The SCIM schema type, see section 2.3 and 7 of RFC7643.
         */
        Type(final String name){
            this.name = name;
        }

        /**
         * Returns the type name as in section 7 of RFC7643.
         * @return A string value
         */
        public String getName()
        {
            return name;
        }

    }

    /**
     * An enum used to describe the mutability of an attribute.
     */
    public enum Mutability{
        /**
         * The attribute can be read, but not written.
         */
        READ_ONLY("readOnly"),
        /**
         * The attribute can be read, and written.
         */
        READ_WRITE("readWrite"),
        /**
         * The attribute can be read, and cannot be set after
         * object creation (unless it was not provided in that moment).
         */
        IMMUTABLE("immutable"),
        /**
         * The attribute can only be written, and not read.  This
         * might be used for passwords for example.
         */
        WRITE_ONLY("writeOnly");

        /**
         * The SCIM name for this enum.
         */
        private String name;

        /**
         * Mutability enum private constructor.
         * @param name The name of the mutability constraint.
         */
        Mutability(final String name){
            this.name = name;
        }

        /**
         * Returns the mutability value (readOnly, readWrite, etc.) as appears in section 7 of RFC7643.
         * @return A string value
         */
        public String getName()
        {
            return name;
        }

    }

    /**
     * This enum is used to describe when/if an attribute should be returned in response to SCIM method invocations.
     */
    public enum Returned{
        /**
         * Indicates that the attribute is always returned.
         */
        ALWAYS("always"),
        /**
         * Indicates that the attribute is never returned.
         */
        NEVER("never"),
        /**
         * Indicates that the attribute is returned by default.
         */
        DEFAULT("default"),
        /**
         * Indicates that the attribute is only returned if requested.
         */
        REQUEST("request");

        /**
         * The SCIM name for this enum.
         */
        private String name;

        /**
         * Returned enum private constructor.
         * @param name the name of the return constraint.
         */
        Returned(final String name){
            this.name = name;
        }

        /**
         * Returns the keyword for the returned characteristic as in section 7 of RFC7643.
         * @return A string value
         */
        public String getName()
        {
            return name;
        }

    }

    /**
     * This enum is used to describe how the service provider enforces uniqueness of attribute values.
     */
    public enum Uniqueness{
        /**
         * Indicates that this attribute's value need not be unique.
         */
        NONE("none"),
        /**
         * Indicates that this attribute's value must be unique for a given server.
         */
        SERVER("server"),
        /**
         * Indicates that this attribute's value must be globally unique.
         */
        GLOBAL("global");

        private String name;

        /**
         * Uniqueness enum private constructor.
         * @param name the name of the uniqueness constraint.
         */
        Uniqueness(final String name){
            this.name = name;
        }

        /**
         * Returns the keyword for the uniqueness characteristic as in section 7 of RFC7643.
         * @return A String value
         */
        public String getName()
        {
            return name;
        }

    }

}
