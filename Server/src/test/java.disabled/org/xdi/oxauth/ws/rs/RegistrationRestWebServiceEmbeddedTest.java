/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.uma.TestUtil;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.util.Util;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * Functional tests for Client Registration Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version 0.9 March 11, 2015
 */
public class RegistrationRestWebServiceEmbeddedTest extends BaseTest {

    private String registrationAccessToken1;
    private String registrationClientUri1;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestClientAssociate1(final String registerPath, final String redirectUris) throws Exception {


                Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

            @Override
            
                try {
                    

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    Response response = request.post(Entity.json(registerRequestContent));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientAssociate1", response, entity);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
                assertNotNull(entity, "Unexpected result: " + entity);
                try {
                    final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
                    ClientTestUtil.assert_(registerResponse);

                    registrationAccessToken1 = registerResponse.getRegistrationAccessToken();
                    registrationClientUri1 = registerResponse.getRegistrationClientUri();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + entity);
                }
            }
        
    }

    @Parameters({"registerPath", "redirectUris", "sectorIdentifierUri", "contactEmail1", "contactEmail2"})
    @Test
    public void requestClientAssociate2(final String registerPath, final String redirectUris,
                                        final String sectorIdentifierUri, final String contactEmail1, final String contactEmail2) throws Exception {


                Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

            @Override
            
                try {
                    

                    List<String> contacts = Arrays.asList(contactEmail1, contactEmail2);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setContacts(contacts);
                    registerRequest.setScopes(Arrays.asList("openid", "clientinfo", "profile", "email", "invalid_scope"));
                    registerRequest.setLogoUri("http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
                    registerRequest.setClientUri("http://www.gluu.org/company/team");
                    registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
                    registerRequest.setPolicyUri("http://www.gluu.org/policy");
                    registerRequest.setJwksUri("http://www.gluu.org/jwks");
                    registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
                    registerRequest.setSubjectType(SubjectType.PAIRWISE);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    Response response = request.post(Entity.json(registerRequestContent));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientAssociate2", response, entity);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
                assertNotNull(entity, "Unexpected result: " + entity);
                try {
                    final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
                    ClientTestUtil.assert_(registerResponse);

                    JSONObject jsonObj = new JSONObject(entity);

                    // Registered Metadata
                    assertTrue(jsonObj.has(CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(SCOPES.toString()));

                    JSONArray scopesJsonArray = jsonObj.getJSONArray(SCOPES.toString());
                    assertEquals(scopesJsonArray.getString(0), "openid");
                    assertEquals(scopesJsonArray.getString(1), "clientinfo");
                    assertEquals(scopesJsonArray.getString(2), "profile");
                    assertEquals(scopesJsonArray.getString(3), "email");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + entity);
                }
            }
        
    }

    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "requestClientAssociate1")
    public void requestClientRead(final String registerPath) throws Exception {


                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            
                

                request.header("Authorization", "Bearer " + registrationAccessToken1);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1));
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientRead", response, entity);
                readResponseAssert(response);
            }
        
    }

    public static void readResponseAssert(EnhancedMockHttpServletResponse response) {
        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(CLIENT_NAME.toString()));
            assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
            assertTrue(jsonObj.has("scopes"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "contactEmail1", "contactEmail2"})
    @Test(dependsOnMethods = "requestClientAssociate1")
    public void requestClientUpdate(final String registerPath, final String redirectUris, final String contactEmail1, final String contactEmail2) throws Exception {
        final String contactEmailNewValue = contactEmail2;
        final String logoUriNewValue = "http://www.gluu.org/test/yuriy/logo.png";
        final String clientUriNewValue = "http://www.gluu.org/company/yuriy";

                ResourceRequestEnvironment.Method.PUT, registerPath) {

            @Override
            
                
                try {

                    final RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));

                    registerRequest.setContacts(Arrays.asList(contactEmail1, contactEmailNewValue));
                    registerRequest.setLogoUri(logoUriNewValue);
                    registerRequest.setClientUri(clientUriNewValue);

                    final String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.header("Authorization", "Bearer " + registrationAccessToken1);
                    request.setContentType(MediaType.APPLICATION_JSON);
                    request.setQueryString(registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1));

                    request.setContent(registerRequestContent.getBytes(Util.UTF8));
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientRead", response, entity);

                readResponseAssert(response);

                try {
                    // check whether values are really updated
                    RegisterRequest r = RegisterRequest.fromJson(entity);
                    assertTrue(r.getContacts() != null && r.getContacts().contains(contactEmailNewValue));
                    assertTrue(r.getClientUri().equals(clientUriNewValue));
                    assertTrue(r.getLogoUri().equals(logoUriNewValue));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + entity);
                }
            }
        
    }

    @Parameters({"registerPath"})
    @Test
    public void requestClientRegistrationFail1(final String registerPath) throws Exception {


                Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

            @Override
            
                try {
                    

                    RegisterRequest registerRequest = new RegisterRequest(null, null, null);

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    Response response = request.post(Entity.json(registerRequestContent));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientRegistrationFail 1", response, entity);

                assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
                TestUtil.assertErrorResponse(entity);
            }
        
    }

    @Parameters({"registerPath"})
    @Test
    public void requestClientRegistrationFail2(final String registerPath) throws Exception {


                Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

            @Override
            
                try {
                    

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null); // Missing redirect URIs

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    Response response = request.post(Entity.json(registerRequestContent));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientRegistrationFail 2", response, entity);

                assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
                TestUtil.assertErrorResponse(entity);
            }
        
    }

    @Parameters({"registerPath"})
    @Test
    public void requestClientRegistrationFail3(final String registerPath) throws Exception {


                Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

            @Override
            
                try {
                    

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            Arrays.asList("https://client.example.com/cb#fail_fragment"));

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    Response response = request.post(Entity.json(registerRequestContent));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            Response response = request.get();
                
                String entity = response.readEntity(String.class);
showResponse("requestClientRegistrationFail3", response, entity);

                assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
                TestUtil.assertErrorResponse(entity);
            }
        
    }
}