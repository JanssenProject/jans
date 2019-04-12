/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.user;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.gluu.persistence.annotation.AttributeEnum;

/**
 * User role
 *
 * @author Yuriy Movchan Date: 11.03.2010
 */
public enum UserRole implements AttributeEnum {

    ADMIN("admin"), OWNER("owner"), MANAGER("manager"), USER("user"), WHITEPAGES("whitePages");

    private String value;

    private static Map<String, UserRole> MAP_BY_VALUES = new HashMap<String, UserRole>();

    static {
        for (UserRole enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getRoleName() {
        return value;
    }

    public String getDisplayName() {
        return value;
    }

    public static UserRole getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public static UserRole[] getByValues(String[] values) {
        UserRole[] roles = new UserRole[values.length];
        for (int i = 0; i < values.length; i++) {
            roles[i] = getByValue(values[i]);
        }

        return roles;
    }

    public static boolean equals(UserRole[] roles1, UserRole[] roles2) {
        Arrays.sort(roles1);
        Arrays.sort(roles2);
        return Arrays.equals(roles1, roles2);
    }

    public static boolean containsRole(UserRole[] roles, UserRole role) {
        if ((roles == null) || (role == null)) {
            return false;
        }

        for (int i = 0; i < roles.length; i++) {
            if (role.equals(roles[i])) {
                return true;
            }
        }

        return false;
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
