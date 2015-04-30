package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LinkedList_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static java.util.LinkedList instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new java.util.LinkedList();
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.util.LinkedList_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.LinkedList_CustomFieldSerializer.deserialize(reader, (java.util.LinkedList)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.LinkedList_CustomFieldSerializer.serialize(writer, (java.util.LinkedList)object);
  }
  
}
