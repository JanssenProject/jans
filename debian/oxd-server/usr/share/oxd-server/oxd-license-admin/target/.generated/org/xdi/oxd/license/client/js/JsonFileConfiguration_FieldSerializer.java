package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class JsonFileConfiguration_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.JsonFileConfiguration instance) throws SerializationException {
    
    org.xdi.oxd.license.client.js.Configuration_FieldSerializer.deserialize(streamReader, instance);
  }
  
  public static org.xdi.oxd.license.client.js.JsonFileConfiguration instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new org.xdi.oxd.license.client.js.JsonFileConfiguration();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.JsonFileConfiguration instance) throws SerializationException {
    
    org.xdi.oxd.license.client.js.Configuration_FieldSerializer.serialize(streamWriter, instance);
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.JsonFileConfiguration_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.JsonFileConfiguration_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.JsonFileConfiguration)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.JsonFileConfiguration_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.JsonFileConfiguration)object);
  }
  
}
