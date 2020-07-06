package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import io.dropwizard.util.Strings;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.Algorithm;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.GetRequestObjectUriParams;
import org.gluu.oxd.common.response.GetRequestObjectUriResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.service.Rp;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class GetRequestObjectUriOperation extends BaseOperation<GetRequestObjectUriParams> {


    private static final Logger LOG = LoggerFactory.getLogger(GetRequestObjectUriOperation.class);

    protected GetRequestObjectUriOperation(Command command, final Injector injector) {
        super(command, injector, GetRequestObjectUriParams.class);
    }

    public IOpResponse execute(GetRequestObjectUriParams params) {

        try {
            validate(params);
            final Rp rp = getRp();

            SignatureAlgorithm algo = SignatureAlgorithm.fromString(params.getRequestObjectSigningAlg()) != null ? SignatureAlgorithm.fromString(params.getRequestObjectSigningAlg()) :
                    SignatureAlgorithm.fromString(rp.getRequestObjectSigningAlg());

            if (algo == null) {
                LOG.error("`request_object_signing_alg` is required parameter in request. Please set this parameter if it is not set during client registration.");
                throw new HttpException(ErrorResponseCode.INVALID_ALGORITHM);
            }

            Jwt unsignedJwt = createRequestObject(algo, rp, params);

            //signing request object
            Jwt signedJwt = getKeyGeneratorService().sign(unsignedJwt, rp.getClientSecret(), algo);

            //setting request object in Expired Object
            String requestUriId = UUID.randomUUID().toString();
            getRequestObjectService().put(requestUriId, signedJwt.toString());

            String requestUri = baseRequestUri(params.getOxdHostUrl()) + requestUriId;
            LOG.trace("RequestObject created successfully. request_uri : {} ", requestUri);

            GetRequestObjectUriResponse response = new GetRequestObjectUriResponse();
            response.setRequestUri(requestUri);
            return response;

        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error in creating `request_uri` response ", e);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_REQUEST_URI);
    }

    public Jwt createRequestObject(SignatureAlgorithm algo, Rp rp, GetRequestObjectUriParams params) {
        Jwt jwt = new Jwt();
        //set header
        jwt.getHeader().setType(JwtType.JWT);
        try {
            jwt.getHeader().setAlgorithm(algo);
            String keyId = getKeyGeneratorService().getKeyId(Algorithm.fromString(algo.getName()), Use.SIGNATURE);
            if (keyId != null) {
                jwt.getHeader().setKeyId(keyId);
            }
        } catch (Exception e) {
            LOG.error("Error in generating key Id.", e);
        }
        //set default claims
        jwt.getClaims().setIssuer(rp.getClientId());
        jwt.getClaims().setAudience(rp.getOpHost());
        jwt.getClaims().setJwtId(UUID.randomUUID().toString());
        jwt.getClaims().setClaim("client_id", rp.getClientId());
        jwt.getClaims().setIssuedAt(new Date());
        jwt.getClaims().setExpirationTime(Utils.addTimeToDate(new Date(), getConfigurationService().getConfiguration().getRequestObjectExpirationInMinutes(), Calendar.MINUTE));
        jwt.getClaims().setClaim("response_type", rp.getResponseTypes());
        jwt.getClaims().setClaim("oxd_id", rp.getOxdId());
        //set claims from params
        if (params.getParams() != null && !params.getParams().isEmpty()) {

            Map<String, Object> claims = params.getParams();
            claims.forEach((key, value) -> {
                if (value instanceof Map) {
                    jwt.getClaims().setClaim(key, (new JSONObject((Map) value)));
                } else {
                    jwt.getClaims().setClaimObject(key, value, true);
                }
            });
        }
        return jwt;
    }

    private void validate(GetRequestObjectUriParams params) {
        if (Strings.isNullOrEmpty(params.getOxdHostUrl())) {
            LOG.error("'oxd_host_url' is empty or not specified.");
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_OXD_HOST);
        }
    }

    private String baseRequestUri(String oxdHost) {
        if (!oxdHost.startsWith("http")) {
            oxdHost = "https://" + oxdHost;
        }
        if (oxdHost.endsWith("/")) {
            oxdHost = StringUtils.removeEnd(oxdHost, "/");
        }
        return oxdHost + "/get-request-object/";
    }
}
