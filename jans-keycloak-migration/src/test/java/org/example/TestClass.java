package org.example;

import jakarta.ws.rs.client.WebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

public class TestClass {

    public static void main(String args[]) throws ClassNotFoundException {

            //try
            //{
              /*  ClassLoader loader = null;
                if (System.getSecurityManager() == null) {
                    loader = Thread.currentThread().getContextClassLoader();
                } else {
                    try {
                        loader = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
                            @Override
                            public ClassLoader run() throws Exception {
                                return Thread.currentThread().getContextClassLoader();
                            }
                        });
                    } catch (PrivilegedActionException pae) {
                        throw new RuntimeException(pae);
                    }
                }
        Class clazz = loader.loadClass("org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl");
        try {
            Constructor c = clazz.getConstructor(Class.class, WebTarget.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }*/

        Keycloak instance = KeycloakBuilder.builder()
                    .serverUrl("http://localhost:8180")
                    .realm("master")
                    .clientId("clientserviceaccount")
                    .clientSecret("cbyFHt3MMU2vNluAmzXopl9SHx9CUmfC")
                    //.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .grantType("client_credentials")
                    .username("admin")
                    .password("keycloak")
                    .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
                    .build();
        //System.out.println("check " + instance.realms());
        List<UserRepresentation> finalUsersResourceList = new ArrayList<>();
        RealmsResource RealmsResource = instance.realms();
        List<RealmRepresentation> RealmResource = RealmsResource.findAll();//realm();

        /*for(RealmRepresentation realmRepresentation : RealmResource){
            System.out.println("check " + realmRepresentation.getRealm());
        }*/
        for(RealmRepresentation realmRepresentation : RealmResource){
            System.out.println("check " + realmRepresentation.getRealm());
            String realm = realmRepresentation.getRealm();
            List<UserRepresentation> usersResourceList = instance.realm(realm).users().list();
            finalUsersResourceList.addAll(usersResourceList);
        }

        for(UserRepresentation userRepresentation : finalUsersResourceList){
            System.out.println("username  " + userRepresentation.getUsername());
        }

       /* TokenManager tokenmanager = instance.tokenManager();
        String accessToken = tokenmanager.getAccessTokenString();
        System.out.println("test : " + accessToken);
        //System.out.println(instance.realm("master").users().searchByUsername("admin",true).get(0).getCredentials());
        List<UserRepresentation> UserRepresentationList = instance.realm("master").users().list();
        System.out.println(UserRepresentationList.get(0).getId());
        System.out.println(instance.realm("master").users().list().get(0));*/
        /*ObjectMapper myObjectMapper = new ObjectMapper();
        myObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        ObjectWriter ow = myObjectMapper.writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(instance.realm("master").users());
        System.out.println("json  : " + json);*/
    }
}
