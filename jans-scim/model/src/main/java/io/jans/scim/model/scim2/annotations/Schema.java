/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate the default schema a SCIM resource belongs to.
 */
/*
 * Created by jgomer on 2017-09-04.
 * Originally based on https://github.com/pingidentity/scim2/blob/master/scim2-sdk-common/src/main/java/com/unboundid/scim2/common/annotations/Schema.java
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Schema{

    /**
     * The schema URN for the resource being annotated
     * @return The resource URN as a String.
     */
    String id();

    /**
     * The description of the resource associated to such URN.
     * @return The resource description.
     */
    String description();

    /**
     * The human readable name of the resource.
     * @return The object's human-readable name.
     */
    String name();

}
