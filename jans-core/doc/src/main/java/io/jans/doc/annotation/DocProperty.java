package io.jans.doc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DocProperty {
    String description() default "None";

    boolean isRequired() default false;

    String defaultValue() default "None";
}
