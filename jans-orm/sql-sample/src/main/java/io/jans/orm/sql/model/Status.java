package io.jans.orm.sql.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
* Status
*
* @author Yuriy Movchan Date: 04/22/2022
*/
public enum Status implements AttributeEnum {

   ACTIVE("active", "Active"), INACTIVE("inactive", "Inactive"), EXPIRED("expired", "Expired"), REGISTER("register", "Register");

   private String value;
   private String displayName;

   private static Map<String, Status> MAP_BY_VALUES = new HashMap<String, Status>();

   static {
       for (Status enumType : values()) {
           MAP_BY_VALUES.put(enumType.getValue(), enumType);
       }
   }

   Status(String value, String displayName) {
       this.value = value;
       this.displayName = displayName;
   }

   public String getValue() {
       return value;
   }

   public String getDisplayName() {
       return displayName;
   }

   public static Status getByValue(String value) {
       return MAP_BY_VALUES.get(value);
   }

   public Enum<? extends AttributeEnum> resolveByValue(String value) {
       return getByValue(value);
   }

   @Override
   public String toString() {
       return value;
   }

}
