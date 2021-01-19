package io.jans.scim2.client.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.scim2.client.ScimClient;
import io.jans.scim2.client.rest.ClientSideService;

/**
 * A factory class to obtain "client" objects that allow interaction with the SCIM service.
 */
public class ScimClientFactory {

    private static Class<ClientSideService> defaultInterface;

    static {
        SecurityProviderUtility.installBCProvider();
        defaultInterface = ClientSideService.class;
    }
    
    /**
     * Constructs an object that allows direct interaction with the SCIM API. Usage examples of this type of client  
     * can be found at https://github.com/JanssenProject/jans-scim/tree/master/README.md
     * @param interfaceClass The Class to which the object returned will belong to. Normally it will be an interface in
     *                       package {@link io.jans.scim2.client.rest gluu.scim2.client.rest} or 
     *                       {@link io.jans.scim.ws.rs.scim2 org.gluu.oxtrust.ws.rs.scim2}
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/jans-scim/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param clientId ID of an already registered OIDC client
     * @param clientSecret Secret of the corresponding client (see clientId parameter)
     * @param <T> The type the object returned will belong to.
     * @return An object that allows to invoke service methods
     * @throws Exception In case of initialization problem
     */
    public static <T> T getClient(Class <T> interfaceClass, String domain, String OIDCMetadataUrl, 
    	String clientId, String clientSecret, boolean secretPostAuthnMethod) throws Exception {
    
        InvocationHandler handler = new ScimClient<>(interfaceClass, domain, OIDCMetadataUrl, 
        	clientId, clientSecret, secretPostAuthnMethod);
        return typedProxy(interfaceClass, handler);
        
    }
    
    public static <T> T getClient(Class <T> interfaceClass, String domain, String OIDCMetadataUrl, 
    	String clientId, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {
    
        InvocationHandler handler = new ScimClient<>(interfaceClass, domain, OIDCMetadataUrl, 
        	clientId, keyStorePath, keyStorePassword, keyId);
        return typedProxy(interfaceClass, handler);
        
    }

    /**
     * Constructs an object that allows direct interaction with the SCIM API. Usage examples of this type of client 
     * can be found at https://github.com/JanssenProject/jans-scim/tree/master/README.md
     * <p>The object returned by this method belongs to interface {@link io.jans.scim2.client.rest.ClientSideService ClientSideService}
     * which has all methods available to interact with User, Group, and FidoDevice SCIM resources. Also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)</p>
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/jans-scim/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param clientId ID of an already registered OIDC client
     * @param clientSecret Secret of the corresponding client (see clientId parameter)
     * @return An object that allows to invoke service methods
     * @throws Exception In case of initialization problem
     */
    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, String clientSecret) throws Exception {
        return getClient(domain, OIDCMetadataUrl, clientId, clientSecret, false);
    }

    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, String clientSecret, boolean secretPostAuthnMethod) throws Exception {
        return getClient(defaultInterface, domain, OIDCMetadataUrl, clientId, clientSecret, secretPostAuthnMethod);
    }

    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, Path keyStorePath, String keyStorePassword) throws Exception {
        return getClient(defaultInterface, domain, OIDCMetadataUrl, clientId, keyStorePath, keyStorePassword, null);
    }

    public static ClientSideService getClient(String domain, String OIDCMetadataUrl, 
    	String clientId, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {
        return getClient(defaultInterface, domain, OIDCMetadataUrl, clientId, keyStorePath, keyStorePassword, keyId);
    }
	
    private static <T> T typedProxy(Class <T> interfaceClass, InvocationHandler handler) {
        return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, handler));
    }

}
