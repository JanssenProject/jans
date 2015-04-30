package com.google.gwt.user.client.rpc.core.java.lang;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class Throwable_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.lang.String getDetailMessage(java.lang.Throwable instance) /*-{
    return instance.@java.lang.Throwable::detailMessage;
  }-*/;
  
  private static native void setDetailMessage(java.lang.Throwable instance, java.lang.String value) 
  /*-{
    instance.@java.lang.Throwable::detailMessage = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, java.lang.Throwable instance) throws SerializationException {
    setDetailMessage(instance, streamReader.readString());
    
  }
  
  public static java.lang.Throwable instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new java.lang.Throwable();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, java.lang.Throwable instance) throws SerializationException {
    streamWriter.writeString(getDetailMessage(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.lang.Throwable_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.Throwable_FieldSerializer.deserialize(reader, (java.lang.Throwable)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.Throwable_FieldSerializer.serialize(writer, (java.lang.Throwable)object);
  }
  
}
