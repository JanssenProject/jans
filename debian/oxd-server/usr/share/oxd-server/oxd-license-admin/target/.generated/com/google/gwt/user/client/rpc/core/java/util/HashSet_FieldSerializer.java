package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class HashSet_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static java.util.HashSet instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new java.util.HashSet();
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.util.HashSet_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.HashSet_CustomFieldSerializer.deserialize(reader, (java.util.HashSet)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.HashSet_CustomFieldSerializer.serialize(writer, (java.util.HashSet)object);
  }
  
}
