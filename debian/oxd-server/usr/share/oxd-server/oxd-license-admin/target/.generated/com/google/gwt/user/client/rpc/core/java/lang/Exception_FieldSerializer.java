package com.google.gwt.user.client.rpc.core.java.lang;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class Exception_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static void deserialize(SerializationStreamReader streamReader, java.lang.Exception instance) throws SerializationException {
    
    com.google.gwt.user.client.rpc.core.java.lang.Throwable_FieldSerializer.deserialize(streamReader, instance);
  }
  
  public static java.lang.Exception instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new java.lang.Exception();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, java.lang.Exception instance) throws SerializationException {
    
    com.google.gwt.user.client.rpc.core.java.lang.Throwable_FieldSerializer.serialize(streamWriter, instance);
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.lang.Exception_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.Exception_FieldSerializer.deserialize(reader, (java.lang.Exception)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.Exception_FieldSerializer.serialize(writer, (java.lang.Exception)object);
  }
  
}
