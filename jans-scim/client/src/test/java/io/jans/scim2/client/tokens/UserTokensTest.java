package io.jans.scim2.client.tokens;

import com.fasterxml.jackson.databind.*;

import io.jans.scim2.client.UserBaseTest;
import io.jans.scim.model.scim2.*;
import io.jans.scim.model.scim2.user.UserResource;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;

import org.testng.ITestContext;
import org.testng.annotations.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

public class UserTokensTest extends UserBaseTest {
    
    private String hash;
    private UserResource user;
    
    private String getAccessToken(Map<String, String> params, String userName, String password) throws Exception {
        
        //Extract token endpoint from metadata URL
        String OIDCMetadataUrl = params.get("OIDCMetadataUrl");
        String clientId = params.get("clientId");
        String secret = params.get("clientSecret");
        String tokenEndpointAuthnMethod = params.get("tokenEndpointAuthnMethod");
        
        JsonNode tree = mapper.readTree(new URL(OIDCMetadataUrl));
        String tokenEndpoint = tree.get("token_endpoint").asText();
        
        WebTarget target = ClientBuilder.newClient().target(tokenEndpoint);
        Form form = new Form().param("grant_type", "password");
        Invocation.Builder builder = target.request();
        
        if (tokenEndpointAuthnMethod.equals("client_secret_basic")) {
            String authz = clientId + ":" + secret;
            authz = new String(Base64.getEncoder().encode(authz.getBytes(UTF_8)), UTF_8);

            builder.header("Authorization", "Basic " + authz);
            
        } else if (tokenEndpointAuthnMethod.equals("client_secret_post")) {
            form.param("client_id", clientId).param("client_secret", secret);            
        } else
            fail("Authentication method " + tokenEndpointAuthnMethod + " not supported");

        form.param("username", userName).param("password", password);
        Response response = builder.post(Entity.form(form));
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.readEntity(String.class)).get("access_token").asText();
        } finally {
            response.close();
        }
		
    }
    
    @Test
    @Parameters("user_minimal_web_login")
    public void retrieve(ITestContext context, String json) throws Exception {
        
        logger.debug("Creating user from json...");
        user = createUserFromJson(json);

        Map<String, String> params = context.getSuite().getXmlSuite().getParameters();
        getAccessToken(params, "jane", "secret");
        getAccessToken(params, "jane", "secret");
        
        //At this point, Jane has to have two tokens associated to her
        List<TokenResource> tokens = getTokens(user.getId());
        assertEquals(tokens.size(), 2);
        logger.debug("Successful retrieval of tokens");

        //Check first one in results
        hash = tokens.get(0).getHash();

    }
    
    @Test(dependsOnMethods = "retrieve")
    public void revoke() {

        String id = user.getId();
        Response response = client.revokeTokens(id, hash + "blah");
        assertEquals(response.getStatus(), NOT_FOUND.getStatusCode());
        
        response = client.revokeTokens(id + "blah", hash);
        assertEquals(response.getStatus(), NOT_FOUND.getStatusCode());
        
        logger.debug("Removing token with hash {} from user {}", hash, id);
        response = client.revokeTokens(id, hash);
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        
        //At this point, Jane has to have one token associated to her
        List<TokenResource> tokens = getTokens(id);
        assertEquals(tokens.size(), 1);
        
        //Check the wiped token is not still there
        assertTrue(hash != tokens.get(0).getHash());

    }
    
    @Test(dependsOnMethods = "revoke")
    public void remove() {
        deleteUser(user);
    }
    
    private List<TokenResource> getTokens(String id) {
        
        Response response = client.getTokensMetadata(id, null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        List<BaseScimResource> tokens = response.readEntity(ListResponse.class).getResources();
        return tokens.stream().map(TokenResource.class::cast).collect(Collectors.toList());
        
    }
    
}
