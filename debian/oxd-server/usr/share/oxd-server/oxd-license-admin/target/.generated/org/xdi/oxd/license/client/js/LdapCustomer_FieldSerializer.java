package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LdapCustomer_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.lang.String getDn(org.xdi.oxd.license.client.js.LdapCustomer instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapCustomer::dn;
  }-*/;
  
  private static native void setDn(org.xdi.oxd.license.client.js.LdapCustomer instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapCustomer::dn = value;
  }-*/;
  
  private static native java.lang.String getId(org.xdi.oxd.license.client.js.LdapCustomer instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapCustomer::id;
  }-*/;
  
  private static native void setId(org.xdi.oxd.license.client.js.LdapCustomer instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapCustomer::id = value;
  }-*/;
  
  private static native java.lang.String getLicenseCryptDN(org.xdi.oxd.license.client.js.LdapCustomer instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapCustomer::licenseCryptDN;
  }-*/;
  
  private static native void setLicenseCryptDN(org.xdi.oxd.license.client.js.LdapCustomer instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapCustomer::licenseCryptDN = value;
  }-*/;
  
  private static native java.lang.String getName(org.xdi.oxd.license.client.js.LdapCustomer instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapCustomer::name;
  }-*/;
  
  private static native void setName(org.xdi.oxd.license.client.js.LdapCustomer instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapCustomer::name = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.LdapCustomer instance) throws SerializationException {
    setDn(instance, streamReader.readString());
    setId(instance, streamReader.readString());
    setLicenseCryptDN(instance, streamReader.readString());
    setName(instance, streamReader.readString());
    
  }
  
  public static org.xdi.oxd.license.client.js.LdapCustomer instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new org.xdi.oxd.license.client.js.LdapCustomer();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.LdapCustomer instance) throws SerializationException {
    streamWriter.writeString(getDn(instance));
    streamWriter.writeString(getId(instance));
    streamWriter.writeString(getLicenseCryptDN(instance));
    streamWriter.writeString(getName(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.LdapCustomer_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapCustomer_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.LdapCustomer)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapCustomer_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.LdapCustomer)object);
  }
  
}
