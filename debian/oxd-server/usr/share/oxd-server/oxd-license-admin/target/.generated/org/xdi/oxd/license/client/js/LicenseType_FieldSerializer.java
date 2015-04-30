package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LicenseType_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.LicenseType instance) throws SerializationException {
    // Enum deserialization is handled via the instantiate method
  }
  
  public static org.xdi.oxd.license.client.js.LicenseType instantiate(SerializationStreamReader streamReader) throws SerializationException {
    int ordinal = streamReader.readInt();
    org.xdi.oxd.license.client.js.LicenseType[] values = org.xdi.oxd.license.client.js.LicenseType.values();
    assert (ordinal >= 0 && ordinal < values.length);
    return values[ordinal];
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.LicenseType instance) throws SerializationException {
    assert (instance != null);
    streamWriter.writeInt(instance.ordinal());
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.LicenseType_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LicenseType_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.LicenseType)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LicenseType_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.LicenseType)object);
  }
  
}
