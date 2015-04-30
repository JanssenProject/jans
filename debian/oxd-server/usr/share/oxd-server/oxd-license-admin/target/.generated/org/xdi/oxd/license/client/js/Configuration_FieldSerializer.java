package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class Configuration_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.lang.String getAuthorizeRequest(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::authorizeRequest;
  }-*/;
  
  private static native void setAuthorizeRequest(org.xdi.oxd.license.client.js.Configuration instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::authorizeRequest = value;
  }-*/;
  
  private static native java.lang.String getBaseDn(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::baseDn;
  }-*/;
  
  private static native void setBaseDn(org.xdi.oxd.license.client.js.Configuration instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::baseDn = value;
  }-*/;
  
  private static native java.lang.String getClientId(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::clientId;
  }-*/;
  
  private static native void setClientId(org.xdi.oxd.license.client.js.Configuration instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::clientId = value;
  }-*/;
  
  private static native java.lang.String getEjbCaWsUrl(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::ejbCaWsUrl;
  }-*/;
  
  private static native void setEjbCaWsUrl(org.xdi.oxd.license.client.js.Configuration instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::ejbCaWsUrl = value;
  }-*/;
  
  private static native java.util.List getLicensePossibleFeatures(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::licensePossibleFeatures;
  }-*/;
  
  private static native void setLicensePossibleFeatures(org.xdi.oxd.license.client.js.Configuration instance, java.util.List value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::licensePossibleFeatures = value;
  }-*/;
  
  private static native java.lang.String getLogoutUrl(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::logoutUrl;
  }-*/;
  
  private static native void setLogoutUrl(org.xdi.oxd.license.client.js.Configuration instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::logoutUrl = value;
  }-*/;
  
  private static native java.lang.Integer getThreadNumberPaidLicense(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::threadNumberPaidLicense;
  }-*/;
  
  private static native void setThreadNumberPaidLicense(org.xdi.oxd.license.client.js.Configuration instance, java.lang.Integer value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::threadNumberPaidLicense = value;
  }-*/;
  
  private static native java.lang.Integer getThreadNumberPremiumLicense(org.xdi.oxd.license.client.js.Configuration instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.Configuration::threadNumberPremiumLicense;
  }-*/;
  
  private static native void setThreadNumberPremiumLicense(org.xdi.oxd.license.client.js.Configuration instance, java.lang.Integer value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.Configuration::threadNumberPremiumLicense = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.Configuration instance) throws SerializationException {
    setAuthorizeRequest(instance, streamReader.readString());
    setBaseDn(instance, streamReader.readString());
    setClientId(instance, streamReader.readString());
    setEjbCaWsUrl(instance, streamReader.readString());
    setLicensePossibleFeatures(instance, (java.util.List) streamReader.readObject());
    setLogoutUrl(instance, streamReader.readString());
    setThreadNumberPaidLicense(instance, (java.lang.Integer) streamReader.readObject());
    setThreadNumberPremiumLicense(instance, (java.lang.Integer) streamReader.readObject());
    
  }
  
  public static org.xdi.oxd.license.client.js.Configuration instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new org.xdi.oxd.license.client.js.Configuration();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.Configuration instance) throws SerializationException {
    streamWriter.writeString(getAuthorizeRequest(instance));
    streamWriter.writeString(getBaseDn(instance));
    streamWriter.writeString(getClientId(instance));
    streamWriter.writeString(getEjbCaWsUrl(instance));
    streamWriter.writeObject(getLicensePossibleFeatures(instance));
    streamWriter.writeString(getLogoutUrl(instance));
    streamWriter.writeObject(getThreadNumberPaidLicense(instance));
    streamWriter.writeObject(getThreadNumberPremiumLicense(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.Configuration_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.Configuration_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.Configuration)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.Configuration_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.Configuration)object);
  }
  
}
