package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class TreeMap_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.util.TreeMap_CustomFieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.TreeMap_CustomFieldSerializer.deserialize(reader, (java.util.TreeMap)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.util.TreeMap_CustomFieldSerializer.serialize(writer, (java.util.TreeMap)object);
  }
  
}
