package gluu.scim2.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.client.*;
import org.gluu.util.StringHelper;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.*;

/**
 * Instances of this class contain the necessary logic to handle the authorization processes required by a client of SCIM
 * service in test mode. For more information on test mode visit the
 * <a href="https://www.gluu.org/docs/ce/user-management/scim2/">SCIM 2.0 docs page</a>.
 * <p><b>Note:</b> Do not instantiate this class in your code. To interact with the service, call the corresponding method in
 * class {@link gluu.scim2.client.factory.ScimClientFactory ScimClientFactory} that returns a proxy object wrapping this client
 * @param <T> Type parameter of superclass
 */
/*
 * Created by jgomer on 2017-07-13.
 */
public class TestModeScimClient<T> extends AbstractScimClient<T> {

    private static final long serialVersionUID = 3141592672017122134L;

    private Logger logger = LogManager.getLogger(getClass());

    //private String authz_code;
    //private String authzEndpoint;
    private String access_token;
    private String refresh_token;

    private String tokenEndpoint;   //Url of authorization's server token endpoint i.e. https://<host:port>/oxauth/restv1/token
    private String registrationEndpoint; //OpenId Connect endpoint for registration, i.e. https://<host:port>/oxauth/restv1/register

    private static long clientExpiration = 0;
    private static String clientId;
    private static String password;
    private static ObjectMapper mapper = new ObjectMapper();

    //private static final List<String> SCOPES=Arrays.asList("openid", "profile");
    private static final List<ResponseType> RESPONSE_TYPES = Collections.singletonList(ResponseType.TOKEN);
    private static final String REDIRECT_URI = "http://localhost/";    //a dummy value just to stay in compliance with specs (see redirect uris for native clients)

    /**
     * Constructs a TestModeScimClient object with the specified parameters and service contract
     *
     * @param serviceClass    The service interface the underlying resteasy proxy client will adhere to. This proxy is used
     *                        internally to execute all requests to the service
     * @param serviceUrl      The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @throws Exception If there was a problem contacting the authorization server to initialize this object
     */
    public TestModeScimClient(Class<T> serviceClass, String serviceUrl, String OIDCMetadataUrl) throws Exception {

        super(serviceUrl, serviceClass);

        //Extract token, registration, and authz endpoints from metadata URL
        JsonNode tree = mapper.readTree(new URL(OIDCMetadataUrl));
        this.registrationEndpoint = tree.get("registration_endpoint").asText();
        this.tokenEndpoint = tree.get("token_endpoint").asText();
        //this.authzEndpoint=tree.get("authorization_endpoint").asText();

        if (StringHelper.isNotEmpty(registrationEndpoint) && StringHelper.isNotEmpty(tokenEndpoint) /*, authzEndpoint*/) {
            triggerRegistrationIfNeeded();
            updateTokens(GrantType.CLIENT_CREDENTIALS);
        } else {
            throw new Exception("Couldn't extract endpoints from OIDC metadata URL: " + OIDCMetadataUrl);
        }

    }

	public TestModeScimClient(Class<T> serviceClass, String serviceUrl, String OIDCMetadataUrl, String id, String secret) throws Exception {
        super(serviceUrl, serviceClass);
        
        //Extract token endpoint from metadata URL
        JsonNode tree = mapper.readTree(new URL(OIDCMetadataUrl));
        this.tokenEndpoint = tree.get("token_endpoint").asText();
        
        if (StringHelper.isNotEmpty(id) && StringHelper.isNotEmpty(secret)) {
        	clientExpiration = Long.MAX_VALUE;
        	clientId = id;
        	password = secret;
        	updateTokens(GrantType.CLIENT_CREDENTIALS);
        } else {
        	throw new Exception("Client ID/secret cannot be empty");
        }
        
	}
        
    private boolean triggerRegistrationIfNeeded() throws Exception {

        boolean flag = false;

        //registration example at org.gluu.oxauth.ws.rs.RegistrationRestWebServiceHttpTest
        if (clientExpiration < System.currentTimeMillis()) {  //registration must take place
            RegisterRequest request = new RegisterRequest(ApplicationType.NATIVE, "SCIM-Client", new ArrayList<>());
            //request.setScopes(SCOPES);
            request.setResponseTypes(RESPONSE_TYPES);
            request.setRedirectUris(Collections.singletonList(REDIRECT_URI));
            request.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            request.setSubjectType(SubjectType.PAIRWISE);
            request.setGrantTypes(Collections.singletonList(GrantType.CLIENT_CREDENTIALS));

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(request);

            RegisterResponse response = registerClient.exec();
            clientId = response.getClientId();
            password = response.getClientSecret();
            clientExpiration = Optional.ofNullable(response.getClientSecretExpiresAt())
            					.map(Date::getTime).orElse(Long.MAX_VALUE);

            flag = true;
        }
        return flag;    //returns if registration was triggered

    }

    private void updateTokens(GrantType grant) {

        access_token = null;
        /*
        Ideally validation of access_token should take place here, however, as no id_token nor refresh_token is issued when
        using Grant type = client credentials (the only applicable for this client - see OAuth2 spec), not much is done here
         */
        //String id_token=response.getIdToken();      //this is null
        //refresh_token = response.getRefreshToken(); //this is null
        access_token = getTokens(grant).getAccessToken();
        logger.debug("Got token: " + access_token);

    }

    private TokenResponse getTokens(GrantType grant) {

        TokenRequest tokenRequest = new TokenRequest(grant);
        //tokenRequest.setScope("openid profile");
        tokenRequest.setAuthUsername(clientId);

        switch (grant) {
            case CLIENT_CREDENTIALS:
                tokenRequest.setAuthPassword(password);
                //tokenRequest.setCode(authz_code);
                break;
            case REFRESH_TOKEN:
                tokenRequest.setRefreshToken(refresh_token);
                //how about refreshing this way: tokenClient.execRefreshToken() ?
                break;
        }
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        return tokenClient.exec();

    }

    /**
     * Builds a string suitable for being passed as an authorization header. It does so by prefixing the current access
     * token this object has with the word "Bearer "
     *
     * @return String built
     */
    @Override
    String getAuthenticationHeader() {
        return "Bearer " + access_token;
    }

    /**
     * Gets a new access token from the authorization server
     *
     * @param response This parameter is not used in practice: there is no need to inspect this value in a setting of
     *                 test mode
     * @return A boolean value indicating the operation was successful
     */
    @Override
    boolean authorize(Response response) {
        /*
        This method is called if the attempt to use the service returned unauthorized (status = 401), so here we check if
        client expired to generate a new one & ask for another token, or else leave it that way (forbidden)
         */
        try {
            triggerRegistrationIfNeeded();
            updateTokens(GrantType.CLIENT_CREDENTIALS);
            //If a new token was asked, an additional call to the service will be made (see method isNeededToAuthorize)
            return (access_token != null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;   //do not make an additional attempt, e.g. getAuthenticationHeader is not called once more
        }

    }

}
