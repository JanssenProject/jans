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
import org.gluu.oxd.common.params.GetRequestUriParams;
import org.gluu.oxd.common.response.GetRequestUriResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.service.Rp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GetRequestUriOperation extends BaseOperation<GetRequestUriParams> {


    private static final Logger LOG = LoggerFactory.getLogger(GetRequestUriOperation.class);

    protected GetRequestUriOperation(Command command, final Injector injector) {
        super(command, injector, GetRequestUriParams.class);
    }

    public IOpResponse execute(GetRequestUriParams params) {

        try {
            validate(params);
            final Rp rp = getRp();

            SignatureAlgorithm algo = SignatureAlgorithm.fromString(params.getRequestObjectSigningAlg()) != null ? SignatureAlgorithm.fromString(params.getRequestObjectSigningAlg()) :
                    SignatureAlgorithm.fromString(rp.getRequestObjectSigningAlg());

            if (algo == null) {
                LOG.trace("The `request_object_signing_alg` parameter is not set. Using `none` algorithm by default.");
                algo = SignatureAlgorithm.fromString("none");
            }

            Jwt jwt = setRequestObject(algo, rp, params);

            //signing request object
            String encodedSignature = getKeyGeneratorService().sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), rp.getClientSecret(), algo);
            LOG.trace("encodedSignature : {} ", encodedSignature);
            jwt.setEncodedSignature(encodedSignature);

            //setting request object in Expired Object
            getStateService().deleteExpiredObjectsByKey(rp.getOxdId());
            getStateService().putRequestObject(rp.getOxdId(), jwt.toString());

            String requestUri = baseOxdUrl(params.getOxdHostUrl(), params.getOxdId());
            LOG.trace("RequestObject created successfully. request_uri : {} ", requestUri);

            GetRequestUriResponse response = new GetRequestUriResponse();
            response.setRequestUri(requestUri);
            return response;

        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error in creating `request_uri` response ", e);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_REQUEST_URI);
    }

    public Jwt setRequestObject(SignatureAlgorithm algo, Rp rp, GetRequestUriParams params) {
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
        //set claims from params
        if (params.getParams() != null && !params.getParams().isEmpty()) {

            Map<String, Object> claims = params.getParams();
            claims.forEach((key, value) -> {

                if (value instanceof String) {
                    jwt.getClaims().setClaim(key, (String) value);
                }
                if (value instanceof Date) {
                    jwt.getClaims().setClaim(key, (Date) value);
                }
                if (value instanceof Boolean) {
                    jwt.getClaims().setClaim(key, (Boolean) value);
                }
                if (value instanceof Integer) {
                    jwt.getClaims().setClaim(key, (Integer) value);
                }
                if (value instanceof Long) {
                    jwt.getClaims().setClaim(key, (Long) value);
                }
                if (value instanceof Character) {
                    jwt.getClaims().setClaim(key, (Character) value);
                }
                if (value instanceof List) {
                    jwt.getClaims().setClaim(key, (List) value);
                }
                if (value instanceof Map) {
                    jwt.getClaims().setClaim(key, (new JSONObject((Map) value)));
                }
                if (value instanceof JSONObject) {
                    jwt.getClaims().setClaim(key, (JSONObject) value);
                }
                if (value instanceof JSONArray) {
                    jwt.getClaims().setClaim(key, (JSONArray) value);
                }
            });
        }
        return jwt;
    }

    private void validate(GetRequestUriParams params) {
        if (Strings.isNullOrEmpty(params.getOxdHostUrl())) {
            LOG.error("'oxd_host_url' is empty or not specified.");
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_OXD_HOST);
        }
    }

    private String baseOxdUrl(String oxdHost, String oxdId) {
        if (!oxdHost.startsWith("http")) {
            oxdHost = "https://" + oxdHost;
        }
        if (oxdHost.endsWith("/")) {
            oxdHost = StringUtils.removeEnd(oxdHost, "/");
        }
        return oxdHost + "/get-request-object-jwt/" + oxdId;
    }
}
