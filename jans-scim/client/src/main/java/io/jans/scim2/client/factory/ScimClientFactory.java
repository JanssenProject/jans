package io.jans.scim2.client.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

import io.jans.scim2.client.ScimClient;
import io.jans.scim2.client.rest.ClientSideService;
import io.jans.util.security.SecurityProviderUtility;

/**
 * A factory class to obtain "client" objects that allow interaction with the SCIM service. Usage
 * examples can be found at https://github.com/JanssenProject/jans-scim/tree/master/README.md
 * <p>Common parameters of methods here include:
 * <ul>
 * <li><code><b>domain</b></code>: The root URL of the SCIM service. Usually in the form <code>https://your.server.com/jans-scim/restv1</code>
 * <li><code><b>OIDCMetadataUrl</b></code>: URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
 * <li><code><b>clientId</b></code>: ID of an already registered OAuth2 client
 * <li><code><b>interfaceClass</b></code>: The Class to which the object returned will belong to. Normally it will be an interface in
 * package {@link io.jans.scim2.client.rest io.jans.scim2.client.rest} or {@link io.jans.scim.ws.rs.scim2 io.jans.scim.ws.rs.scim2}
 * </ul>
 */
public class ScimClientFactory {

    private static Class<ClientSideService> defaultInterface;
    
    private ScimClientFactory() {}

    static {
        SecurityProviderUtility.installBCProvider();
        defaultInterface = ClientSideService.class;
    }
    
    /** 
     * @param interfaceClass See class description
     * @param domain See class description
     * @param OIDCMetadataUrl See class description
     * @param clientId See class description
     * @param clientSecret Secret of the OAuth2 client
     * @param secretPostAuthnMethod Whether the client uses client_secret_post or client_secret_basic
     *                              to authenticate against the token endpoint
     * @param <T> The type the object returned will belong to
     * @return An object that allows to invoke service methods
     * @throws Exception In case of initialization problem
     */
    public static <T> T getClient(Class <T> interfaceClass, String domain, String OIDCMetadataUrl, 
    	String clientId, String clientSecret, boolean secretPostAuthnMethod) throws Exception {
    
        InvocationHandler handler = new ScimClient<>(interfaceClass, domain, OIDCMetadataUrl, 
        	clientId, clientSecret, secretPostAuthnMethod);
        return typedProxy(interfaceClass, handler);
        
    }

    /**
     * @param interfaceClass See class description
     * @param domain See class description
     * @param OIDCMetadataUrl See class description
     * @param clientId See class description. It is assumed the client uses private_key_jwt mechanism to
     *                 authenticate against the token endpoint
     * @param keyStorePath A path to a keystore whose keys may be employed to generate a client_assertion  
     * @param keyStorePassword Password associated to the keystore
     * @param keyId Identifier of one of the keys. Its corresponding private key will be extracted to generate the
     *              assertion. If null is passed, the first key of the keystore will be used  
     * @param <T> The type the object returned will belong to
     * @return An object that allows to invoke service methods
     * @throws Exception In case of initialization problem
     */
    public static <T> T getClient(Class <T> interfaceClass, String domain, String OIDCMetadataUrl, 
    	String clientId, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {
    
        InvocationHandler handler = new ScimClient<>(interfaceClass, domain, OIDCMetadataUrl, 
        	clientId, keyStorePath, keyStorePassword, keyId);
        return typedProxy(interfaceClass, handler);
        
    }

    /**
     * See {@link #getClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean) }
     * @param domain See class description
     * @param OIDCMetadataUrl See class description
     * @param clientId See class description. It is assumed the client uses client_secret_basic mechanism to
     *                 authenticate against the token endpoint
     * @param clientSecret Secret of the OAuth2 client
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception In case of initialization problem
     */
    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, String clientSecret) throws Exception {
        return getClient(domain, OIDCMetadataUrl, clientId, clientSecret, false);
    }

    /**
     * See {@link #getClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean) }
     * @param domain See class description
     * @param OIDCMetadataUrl See class description
     * @param clientId See class description
     * @param clientSecret Secret of the OAuth2 client
     * @param secretPostAuthnMethod Whether the client uses client_secret_post or client_secret_basic
     *                              to authenticate against the token endpoint
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception In case of initialization problem
     */
    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, String clientSecret, boolean secretPostAuthnMethod) throws Exception {
        return getClient(defaultInterface, domain, OIDCMetadataUrl, clientId, clientSecret, secretPostAuthnMethod);
    }
    
    /**
     * See {@link #getClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.nio.file.Path, java.lang.String, java.lang.String) }
     * @param domain See class description
     * @param OIDCMetadataUrl See class description
     * @param clientId See class description. It is assumed the client uses private_key_jwt mechanism to
     *                 authenticate against the token endpoint
     * @param keyStorePath A path to a keystore whose first key may be employed to generate a client_assertion  
     * @param keyStorePassword Password associated to the keystore 
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception In case of initialization problem
     */
    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, Path keyStorePath, String keyStorePassword) throws Exception {
        return getClient(defaultInterface, domain, OIDCMetadataUrl, clientId, keyStorePath,
                keyStorePassword, null);
    }

    /**
     * See {@link #getClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.nio.file.Path, java.lang.String, java.lang.String) }
     * @param domain See class description
     * @param OIDCMetadataUrl See class description
     * @param clientId See class description. It is assumed the client uses private_key_jwt mechanism to
     *                 authenticate against the token endpoint
     * @param keyStorePath A path to a keystore whose keys may be employed to generate a client_assertion  
     * @param keyStorePassword Password associated to the keystore
     * @param keyId Identifier of one of the keys. Its corresponding private key will be extracted to generate the
     *              assertion. If null is passed, the first key of the keystore will be used
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception In case of initialization problem
     */
    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {
        return getClient(defaultInterface, domain, OIDCMetadataUrl, clientId, keyStorePath,
                keyStorePassword, keyId);
    }
	
    private static <T> T typedProxy(Class <T> interfaceClass, InvocationHandler handler) {
        return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, handler));
    }

}
