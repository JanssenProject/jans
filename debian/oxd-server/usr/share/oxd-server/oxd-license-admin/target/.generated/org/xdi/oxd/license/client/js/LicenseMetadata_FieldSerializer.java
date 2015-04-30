package org.xdi.oxd.license.client.js;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class LicenseMetadata_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.util.List getLicenseFeatures(org.xdi.oxd.license.client.js.LicenseMetadata instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LicenseMetadata::licenseFeatures;
  }-*/;
  
  private static native void setLicenseFeatures(org.xdi.oxd.license.client.js.LicenseMetadata instance, java.util.List value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LicenseMetadata::licenseFeatures = value;
  }-*/;
  
  private static native java.lang.String getLicenseName(org.xdi.oxd.license.client.js.LicenseMetadata instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LicenseMetadata::licenseName;
  }-*/;
  
  private static native void setLicenseName(org.xdi.oxd.license.client.js.LicenseMetadata instance, java.lang.String value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LicenseMetadata::licenseName = value;
  }-*/;
  
  private static native org.xdi.oxd.license.client.js.LicenseType getLicenseType(org.xdi.oxd.license.client.js.LicenseMetadata instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LicenseMetadata::licenseType;
  }-*/;
  
  private static native void setLicenseType(org.xdi.oxd.license.client.js.LicenseMetadata instance, org.xdi.oxd.license.client.js.LicenseType value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LicenseMetadata::licenseType = value;
  }-*/;
  
  private static native boolean getMultiServer(org.xdi.oxd.license.client.js.LicenseMetadata instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LicenseMetadata::multiServer;
  }-*/;
  
  private static native void setMultiServer(org.xdi.oxd.license.client.js.LicenseMetadata instance, boolean value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LicenseMetadata::multiServer = value;
  }-*/;
  
  private static native int getThreadsCount(org.xdi.oxd.license.client.js.LicenseMetadata instance) /*-{
    return instance.@org.xdi.oxd.license.client.js.LicenseMetadata::threadsCount;
  }-*/;
  
  private static native void setThreadsCount(org.xdi.oxd.license.client.js.LicenseMetadata instance, int value) 
  /*-{
    instance.@org.xdi.oxd.license.client.js.LicenseMetadata::threadsCount = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, org.xdi.oxd.license.client.js.LicenseMetadata instance) throws SerializationException {
    setLicenseFeatures(instance, (java.util.List) streamReader.readObject());
    setLicenseName(instance, streamReader.readString());
    setLicenseType(instance, (org.xdi.oxd.license.client.js.LicenseType) streamReader.readObject());
    setMultiServer(instance, streamReader.readBoolean());
    setThreadsCount(instance, streamReader.readInt());
    
  }
  
  public static org.xdi.oxd.license.client.js.LicenseMetadata instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new org.xdi.oxd.license.client.js.LicenseMetadata();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, org.xdi.oxd.license.client.js.LicenseMetadata instance) throws SerializationException {
    streamWriter.writeObject(getLicenseFeatures(instance));
    streamWriter.writeString(getLicenseName(instance));
    streamWriter.writeObject(getLicenseType(instance));
    streamWriter.writeBoolean(getMultiServer(instance));
    streamWriter.writeInt(getThreadsCount(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return org.xdi.oxd.license.client.js.LicenseMetadata_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LicenseMetadata_FieldSerializer.deserialize(reader, (org.xdi.oxd.license.client.js.LicenseMetadata)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    org.xdi.oxd.license.client.js.LicenseMetadata_FieldSerializer.serialize(writer, (org.xdi.oxd.license.client.js.LicenseMetadata)object);
  }
  
}
