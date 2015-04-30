package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class Stack_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static void deserialize(SerializationStreamReader streamReader, java.util.Stack instance) throws SerializationException {
    
    com.google.gwt.user.client.rpc.core.java.util.Vector_CustomFieldSerializer.deserialize(streamReader, instance);
  }
  
  public static java.util.Stack instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new java.util.Stack();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, java.util.Stack instance) throws SerializationException {
    
    com.google.gwt.user.client.rpc.core.java.util.Vector_CustomFieldSerializer.serialize(streamWriter, instance);
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.util.Stack_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.Stack_FieldSerializer.deserialize(reader, (java.util.Stack)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.Stack_FieldSerializer.serialize(writer, (java.util.Stack)object);
  }
  
}
