package io.jans.as.client.ws.rs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.register.ApplicationType;
import io.jans.model.authzen.AccessEvaluationRequest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Access Evaluation Endpoint HTTP Test
 *
 * @author Yuriy Z
 */
public class AccessEvaluationHttpTest extends BaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Positive case for access evaluation (accepted by script if subject.type=super_admin)
     */
    @Parameters({"redirectUris"})
    @Test
    public void accessEvaluation_whenSubjectTypeIsAcceptedByScript_shouldGrantAccess(
            final String redirectUris) throws Exception {
        showTitle("accessEvaluation_whenSubjectTypeIsAcceptedByScript_shouldGrantAccess");
        assertNotNull("access_evaluation_v1_endpoint is not set in discovery", accessEvaluationV1Endpoint);

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN);

        List<String> scopes = Arrays.asList("access_evaluation", "openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, scopes);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. request with subject.type=super_admin (in sample demo custom script on AS side we grant access in this case)
        String evaluationRequestJson = "" +
                "{\n" +
                "  \"subject\": {\n" +
                "    \"type\": \"super_admin\",\n" +
                "    \"id\": \"alice@acmecorp.com\"\n" +
                "  },\n" +
                "  \"resource\": {\n" +
                "    \"type\": \"account\",\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"action\": {\n" +
                "    \"name\": \"can_read\",\n" +
                "    \"properties\": {\n" +
                "      \"method\": \"GET\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"context\": {\n" +
                "    \"time\": \"1985-10-26T01:22-07:00\"\n" +
                "  }\n" +
                "}";

        AccessEvaluationClientRequest evaluationRequest = new AccessEvaluationClientRequest();
        evaluationRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        evaluationRequest.setAuthUsername(clientId);
        evaluationRequest.setAuthPassword(clientSecret);
        evaluationRequest.setRequest(MAPPER.readValue(evaluationRequestJson, AccessEvaluationRequest.class));

        AccessEvaluationClient evaluationClient = new AccessEvaluationClient(accessEvaluationV1Endpoint);
        final AccessEvaluationClientResponse evaluationResponse = evaluationClient.exec(evaluationRequest);
        showClient(evaluationClient);

        assertNotNull(evaluationResponse);
        assertTrue(evaluationResponse.getResponse().isDecision());
    }

    /**
     * Negative case for access evaluation (denied by script. Script accept only subject.type=super_admin, here we send subject.type=user)
     */
    @Parameters({"redirectUris"})
    @Test
    public void accessEvaluation_whenSubjectTypeIsNotAcceptedByScript_shouldDenyAccess(
            final String redirectUris) throws Exception {
        showTitle("accessEvaluation_whenSubjectTypeIsNotAcceptedByScript_shouldDenyAccess");
        assertNotNull("access_evaluation_v1_endpoint is not set in discovery", accessEvaluationV1Endpoint);

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN);

        List<String> scopes = Arrays.asList("access_evaluation", "openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, scopes);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. request with subject.type=user (in sample demo custom script on AS side we grant access if subject.type=super_admin, otherwise -> deny)
        String evaluationRequestJson = "" +
                "{\n" +
                "  \"subject\": {\n" +
                "    \"type\": \"user\",\n" +
                "    \"id\": \"alice@acmecorp.com\"\n" +
                "  },\n" +
                "  \"resource\": {\n" +
                "    \"type\": \"account\",\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"action\": {\n" +
                "    \"name\": \"can_read\",\n" +
                "    \"properties\": {\n" +
                "      \"method\": \"GET\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"context\": {\n" +
                "    \"time\": \"1985-10-26T01:22-07:00\"\n" +
                "  }\n" +
                "}";

        AccessEvaluationClientRequest evaluationRequest = new AccessEvaluationClientRequest();
        evaluationRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        evaluationRequest.setAuthUsername(clientId);
        evaluationRequest.setAuthPassword(clientSecret);
        evaluationRequest.setRequest(MAPPER.readValue(evaluationRequestJson, AccessEvaluationRequest.class));

        AccessEvaluationClient evaluationClient = new AccessEvaluationClient(accessEvaluationV1Endpoint);
        final AccessEvaluationClientResponse evaluationResponse = evaluationClient.exec(evaluationRequest);
        showClient(evaluationClient);

        assertNotNull(evaluationResponse);
        assertFalse(evaluationResponse.getResponse().isDecision());
    }

    public RegisterResponse registerClient(final String redirectUris, List<ResponseType> responseTypes, List<GrantType> grantTypes, List<String> scopes) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "access_evaluation test",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }
}
