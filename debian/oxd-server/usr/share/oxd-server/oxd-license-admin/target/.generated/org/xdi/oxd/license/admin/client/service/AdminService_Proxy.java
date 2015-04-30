package org.xdi.oxd.license.admin.client.service;

import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.RpcToken;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;

public class AdminService_Proxy extends RemoteServiceProxy implements org.xdi.oxd.license.admin.client.service.AdminServiceAsync {
  private static final String REMOTE_SERVICE_INTERFACE_NAME = "org.xdi.oxd.license.admin.client.service.AdminService";
  private static final String SERIALIZATION_POLICY ="4E4C93F5C28080A829EDE96D916E0004";
  private static final org.xdi.oxd.license.admin.client.service.AdminService_TypeSerializer SERIALIZER = new org.xdi.oxd.license.admin.client.service.AdminService_TypeSerializer();
  
  public AdminService_Proxy() {
    super(GWT.getModuleBaseURL(),
      "adminService.rpc", 
      SERIALIZATION_POLICY, 
      SERIALIZER);
  }
  
  public void generate(com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "generate");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 0);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void generateLicenseIds(int count, org.xdi.oxd.license.client.js.LdapLicenseCrypt licenseCrypt, org.xdi.oxd.license.client.js.LicenseMetadata metadata, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "generateLicenseIds");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 3);
      streamWriter.writeString("I");
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapLicenseCrypt/2742937958");
      streamWriter.writeString("org.xdi.oxd.license.client.js.LicenseMetadata/1513233265");
      streamWriter.writeInt(count);
      streamWriter.writeObject(licenseCrypt);
      streamWriter.writeObject(metadata);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void getAllCustomers(com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "getAllCustomers");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 0);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void getAllLicenseCryptObjects(com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "getAllLicenseCryptObjects");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 0);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void getConfiguration(com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "getConfiguration");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 0);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void getLicenseCrypt(java.lang.String dn, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "getLicenseCrypt");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("java.lang.String/2004016611");
      streamWriter.writeString(dn);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void loadLicenseIdsByCrypt(org.xdi.oxd.license.client.js.LdapLicenseCrypt licenseCrypt, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "loadLicenseIdsByCrypt");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapLicenseCrypt/2742937958");
      streamWriter.writeObject(licenseCrypt);
      helper.finish(async, ResponseReader.OBJECT);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void remove(java.util.Collection entities, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "remove");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("java.util.Collection");
      streamWriter.writeObject(entities);
      helper.finish(async, ResponseReader.VOID);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void remove(org.xdi.oxd.license.client.js.LdapCustomer entity, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "remove");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapCustomer/1903104965");
      streamWriter.writeObject(entity);
      helper.finish(async, ResponseReader.VOID);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void remove(org.xdi.oxd.license.client.js.LdapLicenseCrypt entity, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "remove");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapLicenseCrypt/2742937958");
      streamWriter.writeObject(entity);
      helper.finish(async, ResponseReader.VOID);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void save(org.xdi.oxd.license.client.js.LdapCustomer customer, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "save");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapCustomer/1903104965");
      streamWriter.writeObject(customer);
      helper.finish(async, ResponseReader.VOID);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void save(org.xdi.oxd.license.client.js.LdapLicenseCrypt customer, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "save");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapLicenseCrypt/2742937958");
      streamWriter.writeObject(customer);
      helper.finish(async, ResponseReader.VOID);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  
  public void save(org.xdi.oxd.license.client.js.LdapLicenseId entity, com.google.gwt.user.client.rpc.AsyncCallback async) {
    com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper helper = new com.google.gwt.user.client.rpc.impl.RemoteServiceProxy.ServiceHelper("AdminService_Proxy", "save");
    try {
      SerializationStreamWriter streamWriter = helper.start(REMOTE_SERVICE_INTERFACE_NAME, 1);
      streamWriter.writeString("org.xdi.oxd.license.client.js.LdapLicenseId/4184820819");
      streamWriter.writeObject(entity);
      helper.finish(async, ResponseReader.VOID);
    } catch (SerializationException ex) {
      async.onFailure(ex);
    }
  }
  @Override
  public SerializationStreamWriter createStreamWriter() {
    ClientSerializationStreamWriter toReturn =
      (ClientSerializationStreamWriter) super.createStreamWriter();
    if (getRpcToken() != null) {
      toReturn.addFlags(ClientSerializationStreamWriter.FLAG_RPC_TOKEN_INCLUDED);
    }
    return toReturn;
  }
  @Override
  protected void checkRpcTokenType(RpcToken token) {
    if (!(token instanceof com.google.gwt.user.client.rpc.XsrfToken)) {
      throw new RpcTokenException("Invalid RpcToken type: expected 'com.google.gwt.user.client.rpc.XsrfToken' but got '" + token.getClass() + "'");
    }
  }
}
