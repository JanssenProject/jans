package com.google.gwt.user.client.rpc.core.java.lang;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class String_Array_Rank_1_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static void deserialize(SerializationStreamReader streamReader, java.lang.String[] instance) throws SerializationException {
    for (int i = 0, n = instance.length; i < n; ++i) {
      instance[i] = streamReader.readString();
    }
  }
  
  public static java.lang.String[] instantiate(SerializationStreamReader streamReader) throws SerializationException {
    int size = streamReader.readInt();
    return new java.lang.String[size];
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, java.lang.String[] instance) throws SerializationException {
    streamWriter.writeInt(instance.length);
    
    for (int i = 0, n = instance.length; i < n; ++i) {
      streamWriter.writeString(instance[i]);
    }
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return com.google.gwt.user.client.rpc.core.java.lang.String_Array_Rank_1_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.String_Array_Rank_1_FieldSerializer.deserialize(reader, (java.lang.String[])object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.String_Array_Rank_1_FieldSerializer.serialize(writer, (java.lang.String[])object);
  }
  
}
