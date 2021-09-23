package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.dropwizard.util.Strings;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoRequest;
import io.jans.as.client.UserInfoResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetUserInfoParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetUserInfoOperation extends BaseOperation<GetUserInfoParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetUserInfoOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetUserInfoOperation(Command command, final Injector injector) {
        super(command, injector, GetUserInfoParams.class);
    }

    @Override
    public IOpResponse execute(GetUserInfoParams params) throws IOException {
        getValidationService().validate(params);

        UserInfoClient client = getOpClientFactory().createUserInfoClient(getDiscoveryService().getConnectDiscoveryResponseByRpId(params.getRpId()).getUserInfoEndpoint());
        client.setExecutor(getHttpService().getClientExecutor());
        client.setRequest(new UserInfoRequest(params.getAccessToken()));

        final UserInfoResponse response = client.exec();
        //validate subject identifier of successful response
        if (response.getStatus() == 200) {
            validateSubjectIdentifier(params.getIdToken(), response);
        }

        return new POJOResponse(Jackson2.createJsonMapper().readTree(response.getEntity()));
    }

    public void validateSubjectIdentifier(String idToken, UserInfoResponse response) {
        try {
            boolean validateUserInfoWithIdToken = getConfigurationService().getConfiguration().getValidateUserInfoWithIdToken();
            if (!validateUserInfoWithIdToken) {
                return;
            }

            if (Strings.isNullOrEmpty(idToken)) {
                return;
            }
            LOG.trace("Validating subject Identifier (`sub`) of userInfo response.");
            String subjectIdentifier = response.getClaims().get("sub");
            final Jwt jwtIdToken = Jwt.parse(idToken);
            if (!jwtIdToken.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER).equals(subjectIdentifier)) {
                LOG.error("UserInfo `sub` value does not matches with `sub` value of ID_TOKEN.\n ID_TOKEN `sub`: {}  \n UserInfo `sub`: {} ", jwtIdToken.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER), subjectIdentifier);
                throw new HttpException(ErrorResponseCode.INVALID_SUBJECT_IDENTIFIER);
            }
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error in matching `sub` value of UserInfo with `sub` value of ID_TOKEN.", e);
            throw new HttpException(ErrorResponseCode.FAILED_TO_VERIFY_SUBJECT_IDENTIFIER);
        }
    }
}
