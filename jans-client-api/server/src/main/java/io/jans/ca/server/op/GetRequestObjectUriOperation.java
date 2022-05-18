package io.jans.ca.server.op;

import com.google.common.base.Strings;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetRequestObjectUriParams;
import io.jans.ca.common.response.GetRequestObjectUriResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.ServiceProvider;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class GetRequestObjectUriOperation extends BaseOperation<GetRequestObjectUriParams> {


    private static final Logger LOG = LoggerFactory.getLogger(GetRequestObjectUriOperation.class);

    public GetRequestObjectUriOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetRequestObjectUriParams.class);
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

            String requestUri = baseRequestUri(params.getRpHostUrl()) + requestUriId;
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
        jwt.getClaims().setExpirationTime(Utils.addTimeToDate(new Date(), getJansConfigurationService().find().getRequestObjectExpirationInMinutes(), Calendar.MINUTE));
        jwt.getClaims().setClaim("response_type", rp.getResponseTypes());
        jwt.getClaims().setClaim("rp_id", rp.getRpId());
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
        if (Strings.isNullOrEmpty(params.getRpHostUrl())) {
            LOG.error("'rp_host_url' is empty or not specified.");
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RP_HOST);
        }
    }

    private String baseRequestUri(String rpHost) {
        if (!rpHost.startsWith("http")) {
            rpHost = "https://" + rpHost;
        }
        if (rpHost.endsWith("/")) {
            rpHost = StringUtils.removeEnd(rpHost, "/");
        }
        return rpHost + "/api/get-request-object/";
    }
}
