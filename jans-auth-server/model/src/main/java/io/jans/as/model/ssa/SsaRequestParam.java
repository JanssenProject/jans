/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.ssa;

public enum SsaRequestParam {

    ORG_ID("org_id"),
    EXPIRATION("expiration"),
    DESCRIPTION("description"),
    SOFTWARE_ID("software_id"),
    SOFTWARE_ROLES("software_roles"),
    GRANT_TYPES("grant_types"),
    ONE_TIME_USE("one_time_use"),
    ROTATE_SSA("rotate_ssa"),

    CREATED_AT("created_at"),
    ISSUER("iss"),
    JTI("jti"),
    ISS("iss"),
    EXP("exp"),
    IAT("iat"),
    SSA("ssa"),
    STATUS("status"),
    ;

    private final String name;

    SsaRequestParam(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}