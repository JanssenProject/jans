package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LdapLicenseCrypt_Array_Rank_1_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.LdapLicenseCrypt[] instance) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.Object_Array_CustomFieldSerializer.deserialize(streamReader, instance);
  }
  
  public static org.xdi.oxd.license.client.js.LdapLicenseCrypt[] instantiate(SerializationStreamReader streamReader) throws SerializationException {
    int size = streamReader.readInt();
    return new org.xdi.oxd.license.client.js.LdapLicenseCrypt[size];
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.LdapLicenseCrypt[] instance) throws SerializationException {
    com.google.gwt.user.client.rpc.core.java.lang.Object_Array_CustomFieldSerializer.serialize(streamWriter, instance);
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.LdapLicenseCrypt_Array_Rank_1_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapLicenseCrypt_Array_Rank_1_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.LdapLicenseCrypt[])object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapLicenseCrypt_Array_Rank_1_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.LdapLicenseCrypt[])object);
  }
  
}
