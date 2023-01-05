/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.json;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum PropertyDefinition {

    ADDITIONAL_AUDIENCE(Sets.newHashSet(ClassNames.CLIENT_ATTRIBUTES, ClassNames.REGISTER_REQUEST), "additionalAudience", List.class, "additional_audience");

    private final Set<ClassNames> javaTargetsClassNames;
    private final Set<String> javaTargetsClassNamesAsStrings;
    private final String javaTargetPropertyName;
    private final Class<?> javaType;
    private final String jsonName;


    PropertyDefinition(Set<ClassNames> javaTargetsClassNames, String javaTargetPropertyName, Class<?> javaType, String jsonName) {
        this.javaTargetsClassNames = javaTargetsClassNames;
        this.javaTargetsClassNamesAsStrings = javaTargetsClassNames.stream().map(PropertyDefinition.ClassNames::getFullClassName).collect(Collectors.toSet());
        this.javaTargetPropertyName = javaTargetPropertyName;
        this.javaType = javaType;
        this.jsonName = jsonName;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public Set<ClassNames> getJavaTargetsClassNames() {
        return javaTargetsClassNames;
    }

    public Set<String> getJavaTargetsClassNamesAsStrings() {
        return javaTargetsClassNamesAsStrings;
    }

    public String getJavaTargetPropertyName() {
        return javaTargetPropertyName;
    }

    public String getJsonName() {
        return jsonName;
    }

    @Override
    public String toString() {
        return "PropertyDefinition{" +
                "javaTargetsClassNames='" + javaTargetsClassNames + '\'' +
                ", javaTargetPropertyName='" + javaTargetPropertyName + '\'' +
                ", javaType='" + javaType + '\'' +
                ", jsonName='" + jsonName + '\'' +
                "} " + super.toString();
    }

    public enum ClassNames {

        CLIENT_ATTRIBUTES("io.jans.as.persistence.model.ClientAttributes"),
        REGISTER_REQUEST("io.jans.as.client.RegisterRequest");

        private final String fullClassName;

        ClassNames(String fullClassName) {
            this.fullClassName = fullClassName;
        }

        public String getFullClassName() {
            return fullClassName;
        }
    }
}