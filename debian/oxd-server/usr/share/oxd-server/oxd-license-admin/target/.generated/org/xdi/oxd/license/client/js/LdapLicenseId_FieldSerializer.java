package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LdapLicenseId_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.lang.String getDn(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::dn;
  }-*/;
  
  private static native void setDn(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::dn = value;
  }-*/;
  
  private static native java.lang.Boolean getForceLicenseUpdate(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::forceLicenseUpdate;
  }-*/;
  
  private static native void setForceLicenseUpdate(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.lang.Boolean value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::forceLicenseUpdate = value;
  }-*/;
  
  private static native java.lang.String getLicenseCryptDN(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licenseCryptDN;
  }-*/;
  
  private static native void setLicenseCryptDN(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licenseCryptDN = value;
  }-*/;
  
  private static native java.lang.String getLicenseId(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licenseId;
  }-*/;
  
  private static native void setLicenseId(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licenseId = value;
  }-*/;
  
  private static native java.util.List getLicenses(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licenses;
  }-*/;
  
  private static native void setLicenses(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.util.List value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licenses = value;
  }-*/;
  
  private static native java.lang.Integer getLicensesIssuedCount(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licensesIssuedCount;
  }-*/;
  
  private static native void setLicensesIssuedCount(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.lang.Integer value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::licensesIssuedCount = value;
  }-*/;
  
  private static native java.lang.String getMetadata(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::metadata;
  }-*/;
  
  private static native void setMetadata(org.xdi.oxd.license.client.js.LdapLicenseId instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::metadata = value;
  }-*/;
  
  private static native org.xdi.oxd.license.client.js.LicenseMetadata getMetadataAsObject(org.xdi.oxd.license.client.js.LdapLicenseId instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseId::metadataAsObject;
  }-*/;
  
  private static native void setMetadataAsObject(org.xdi.oxd.license.client.js.LdapLicenseId instance, org.xdi.oxd.license.client.js.LicenseMetadata value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseId::metadataAsObject = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.LdapLicenseId instance) throws SerializationException {
    setDn(instance, streamReader.readString());
    setForceLicenseUpdate(instance, (java.lang.Boolean) streamReader.readObject());
    setLicenseCryptDN(instance, streamReader.readString());
    setLicenseId(instance, streamReader.readString());
    setLicenses(instance, (java.util.List) streamReader.readObject());
    setLicensesIssuedCount(instance, (java.lang.Integer) streamReader.readObject());
    setMetadata(instance, streamReader.readString());
    setMetadataAsObject(instance, (org.xdi.oxd.license.client.js.LicenseMetadata) streamReader.readObject());
    
  }
  
  public static org.xdi.oxd.license.client.js.LdapLicenseId instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new org.xdi.oxd.license.client.js.LdapLicenseId();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.LdapLicenseId instance) throws SerializationException {
    streamWriter.writeString(getDn(instance));
    streamWriter.writeObject(getForceLicenseUpdate(instance));
    streamWriter.writeString(getLicenseCryptDN(instance));
    streamWriter.writeString(getLicenseId(instance));
    streamWriter.writeObject(getLicenses(instance));
    streamWriter.writeObject(getLicensesIssuedCount(instance));
    streamWriter.writeString(getMetadata(instance));
    streamWriter.writeObject(getMetadataAsObject(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.LdapLicenseId_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapLicenseId_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.LdapLicenseId)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapLicenseId_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.LdapLicenseId)object);
  }
  
}
