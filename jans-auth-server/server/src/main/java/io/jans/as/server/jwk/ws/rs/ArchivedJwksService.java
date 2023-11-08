package io.jans.as.server.jwk.ws.rs;

import io.jans.as.common.model.common.ArchivedJwk;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Named
public class ArchivedJwksService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    public String buildDn(String id) {
        return String.format("jansId=%s,%s", id, staticConfiguration.getBaseDn().getArchivedJwks());
    }

    public ArchivedJwk getArchivedJwkByDn(String dn) {
        try {
            return persistenceEntryManager.find(ArchivedJwk.class, dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    public Response requestArchivedKid(String kid) {
        log.debug("Requesting archived kid {} ...", kid);

        final ArchivedJwk archivedJwk = getArchivedJwk(kid);

        if (archivedJwk == null) {
            log.trace("Unable to find archived jwk by kid {}", kid);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, ""))
                    .build());
        }

        final String entity = JSONWebKeySet.toPrettyString(archivedJwk.getData());

        if (log.isTraceEnabled()) {
            log.trace("Returned archived jwk, kid: {}, entity: {}", kid, entity);
        }

        return Response.ok()
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(entity)
                .build();
    }

    public ArchivedJwk getArchivedJwk(String kid) {
        if (StringUtils.isNotBlank(kid)) {
            ArchivedJwk result = getArchivedJwkByDn(buildDn(kid));
            log.debug("Found {} entries for ArchivedJwk id = {}", result != null ? 1 : 0, kid);

            return result;
        }
        return null;
    }

    public void persist(ArchivedJwk entity) {
        persistenceEntryManager.persist(entity);
    }

    public void merge(ArchivedJwk entity) {
        persistenceEntryManager.merge(entity);
    }
}
