package io.jans.util.properties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface JansConfigProperty {
    String description() default "None";

    boolean isMandatory() default false;

    String defaultValue() default "None";
}
