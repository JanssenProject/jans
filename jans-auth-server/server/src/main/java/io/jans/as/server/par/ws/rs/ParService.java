package io.jans.as.server.par.ws.rs;

import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.orm.PersistenceEntryManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ParService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    public static String toPersistenceId(String id) {
        return StringUtils.replace(id, Util.PAR_ID_REFIX, Util.PAR_ID_SHORT_REFIX);
    }

    public static String toOutsideId(String id) {
        return StringUtils.replace(id, Util.PAR_ID_SHORT_REFIX, Util.PAR_ID_REFIX);
    }

    public void persist(Par par) {
        setIdAndDnIfNeeded(par);

        entryManager.persist(par);
    }

    public Par getPar(String id) {
        return getParByDn(dn(id));
    }

    public Par getParByDn(String dn) {
        try {
            return entryManager.find(Par.class, dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    private void setIdAndDnIfNeeded(Par par) {
        if (StringUtils.isBlank(par.getId())) {
            par.setId(Util.PAR_ID_SHORT_REFIX + UUID.randomUUID().toString());
        }

        if (StringUtils.isBlank(par.getDn())) {
            par.setDn(dn(par.getId()));
        }
    }

    public String dn(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("PAR id is null or blank.");
        }
        return String.format("jansId=%s,%s", toPersistenceId(id), branchBaseDn());
    }

    public String branchBaseDn() {
        return staticConfiguration.getBaseDn().getPar(); // "ou=par,o=jans"
    }

    public Par getParAndValidateForAuthorizationRequest(String id, String state, String clientIdInRequest) {
        Par par = getPar(id);

        if (par == null) {
            log.debug("Failed to find PAR by request_uri (id): {}", id);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Failed to find par by request_uri"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
        if (StringUtils.isBlank(clientIdInRequest) || !clientIdInRequest.equals(par.getAttributes().getClientId())) {
            log.debug("client_id does not match to PAR's client_id (used during PAR registration). Reject request. PAR clientId: {}, request's clientId: {}", par.getAttributes().getClientId(), clientIdInRequest);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "client_id does not match to PAR's client_id"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        validate(par, state);
        return par;
    }

    private void validate(Par par, String state) {
        Date now = new Date();
        if (par.isExpired(now)) {
            log.debug("PAR is expired, id: {}, exp: {}, now: {}", par.getId(), par.getExpirationDate(), now);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST_URI, state, "PAR is expired"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        try {
            JwtAuthorizationRequest.validateExp((int) (par.getExpirationDate().getTime() / 1000));
            JwtAuthorizationRequest.validateNbf(par.getAttributes().getNbf());
        } catch (InvalidJwtException e) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Failed to validate exp or nbf"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
    }
}
