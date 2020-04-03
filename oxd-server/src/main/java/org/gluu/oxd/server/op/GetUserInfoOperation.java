package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.gluu.oxauth.client.UserInfoClient;
import org.gluu.oxauth.client.UserInfoRequest;
import org.gluu.oxauth.client.UserInfoResponse;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.GetUserInfoParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.Rp;
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

        UserInfoClient client = getOpClientFactory().createUserInfoClient(getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId()).getUserInfoEndpoint());
        client.setExecutor(getHttpService().getClientExecutor());
        client.setRequest(new UserInfoRequest(params.getAccessToken()));

        final UserInfoResponse response = client.exec();
        //validate subject identifier of successful response
        if (response.getStatus() == 200) {
            final Rp rp = getRp();
            validateSubjectIdentifier(rp.getIdToken(), response);
        }

        return new POJOResponse(Jackson2.createJsonMapper().readTree(response.getEntity()));
    }

    public void validateSubjectIdentifier(String idToken, UserInfoResponse response) {
        try {
            String subjectIdentifier = response.getClaims().get("sub").get(0);
            final Jwt jwtIdToken = Jwt.parse(idToken);
            if (!jwtIdToken.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER).equals(subjectIdentifier)) {
                LOG.error("UserInfo `sub` value does not matches with `sub` value of ID_TOKEN.");
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
