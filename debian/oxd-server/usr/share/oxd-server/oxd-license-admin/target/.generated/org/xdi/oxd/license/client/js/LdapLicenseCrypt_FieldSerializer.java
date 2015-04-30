package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LdapLicenseCrypt_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.lang.String getClientPrivateKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::clientPrivateKey;
  }-*/;
  
  private static native void setClientPrivateKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::clientPrivateKey = value;
  }-*/;
  
  private static native java.lang.String getClientPublicKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::clientPublicKey;
  }-*/;
  
  private static native void setClientPublicKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::clientPublicKey = value;
  }-*/;
  
  private static native java.lang.String getDn(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::dn;
  }-*/;
  
  private static native void setDn(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::dn = value;
  }-*/;
  
  private static native java.lang.String getId(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::id;
  }-*/;
  
  private static native void setId(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::id = value;
  }-*/;
  
  private static native java.lang.String getLicensePassword(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::licensePassword;
  }-*/;
  
  private static native void setLicensePassword(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::licensePassword = value;
  }-*/;
  
  private static native java.lang.String getName(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::name;
  }-*/;
  
  private static native void setName(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::name = value;
  }-*/;
  
  private static native java.lang.String getPrivateKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::privateKey;
  }-*/;
  
  private static native void setPrivateKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::privateKey = value;
  }-*/;
  
  private static native java.lang.String getPrivatePassword(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::privatePassword;
  }-*/;
  
  private static native void setPrivatePassword(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::privatePassword = value;
  }-*/;
  
  private static native java.lang.String getPublicKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::publicKey;
  }-*/;
  
  private static native void setPublicKey(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::publicKey = value;
  }-*/;
  
  private static native java.lang.String getPublicPassword(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::publicPassword;
  }-*/;
  
  private static native void setPublicPassword(org.xdi.oxd.license.client.js.LdapLicenseCrypt instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LdapLicenseCrypt::publicPassword = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) throws SerializationException {
    setClientPrivateKey(instance, streamReader.readString());
    setClientPublicKey(instance, streamReader.readString());
    setDn(instance, streamReader.readString());
    setId(instance, streamReader.readString());
    setLicensePassword(instance, streamReader.readString());
    setName(instance, streamReader.readString());
    setPrivateKey(instance, streamReader.readString());
    setPrivatePassword(instance, streamReader.readString());
    setPublicKey(instance, streamReader.readString());
    setPublicPassword(instance, streamReader.readString());
    
  }
  
  public static org.xdi.oxd.license.client.js.LdapLicenseCrypt instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new org.xdi.oxd.license.client.js.LdapLicenseCrypt();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.LdapLicenseCrypt instance) throws SerializationException {
    streamWriter.writeString(getClientPrivateKey(instance));
    streamWriter.writeString(getClientPublicKey(instance));
    streamWriter.writeString(getDn(instance));
    streamWriter.writeString(getId(instance));
    streamWriter.writeString(getLicensePassword(instance));
    streamWriter.writeString(getName(instance));
    streamWriter.writeString(getPrivateKey(instance));
    streamWriter.writeString(getPrivatePassword(instance));
    streamWriter.writeString(getPublicKey(instance));
    streamWriter.writeString(getPublicPassword(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.LdapLicenseCrypt_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapLicenseCrypt_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.LdapLicenseCrypt)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LdapLicenseCrypt_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.LdapLicenseCrypt)object);
  }
  
}
