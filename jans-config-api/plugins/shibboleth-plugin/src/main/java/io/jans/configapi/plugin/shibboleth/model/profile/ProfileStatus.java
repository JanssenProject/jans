package io.jans.configapi.plugin.shibboleth.model.profile;

import com.fasterxml.jackson.annotation.JsonValue;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

public enum ProfileStatus implements AttributeEnum{

    ACTIVE("active", "Active"), INACTIVE("inactive", "Inactive");

   private String value;
   private String displayName;

   private static Map<String, ProfileStatus> MAP_BY_VALUES = new HashMap<String, ProfileStatus>();

   static {
       for (ProfileStatus enumType : values()) {
           MAP_BY_VALUES.put(enumType.getValue(), enumType);
       }
   }

   ProfileStatus(String value, String displayName) {
       this.value = value;
       this.displayName = displayName;
   }

   public String getValue() {
       return value;
   }

   public String getDisplayName() {
       return displayName;
   }

   public static ProfileStatus getByValue(String value) {
       return MAP_BY_VALUES.get(value);
   }

   public Enum<? extends AttributeEnum> resolveByValue(String value) {
       return getByValue(value);
   }

   @Override
   @JsonValue
   public String toString() {
       return value;
   }
}