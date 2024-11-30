package io.jans.as.server.jwk.ws.rs;

import io.jans.as.common.model.common.ArchivedJwk;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static io.jans.as.model.jwk.JWKParameter.KEY_ID;

/**
 * @author Yuriy Z
 */
@Named
public class ArchivedJwksService {

    public static final int SECONDS_IN_ONE_YEAR = 31536000;

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

    public void archiveJwk(JSONObject keyAsJson) {
        if (keyAsJson == null) {
            log.trace("JWK is null, skip archiving.");
            return;
        }

        try {
            JSONWebKey jwk = JSONWebKey.fromJSONObject(keyAsJson);

            final String kid = jwk.getKid();

            final ArchivedJwk existing = getArchivedJwk(kid);
            if (existing != null) {
                log.debug("JWK {} already archived.", kid);
                return;
            }

            log.debug("Trying to archive jwk {} ...", kid);

            ArchivedJwk archivedJwk = new ArchivedJwk();
            archivedJwk.setDn(buildDn(kid));
            archivedJwk.setId(kid);
            archivedJwk.setData(keyAsJson);
            archivedJwk.setCreationDate(new Date());
            archivedJwk.setDeletable(true);
            archivedJwk.setExpirationDate(getExpirationDate());
            archivedJwk.setTtl(getLifetimeInSeconds());

            persist(archivedJwk);

            log.debug("Archived jwk {} successfully.", kid);
        } catch (Exception e) {
            log.error("Failed to archive jwk: {}", keyAsJson);
        }
    }

    public int getLifetimeInSeconds() {
        final int lifetimeFromConfig = appConfiguration.getArchivedJwkLifetimeInSeconds();
        if (lifetimeFromConfig > 0) {
            return lifetimeFromConfig;
        }
        return SECONDS_IN_ONE_YEAR;
    }

    private Date getExpirationDate() {
        int lifetimeInSeconds = getLifetimeInSeconds();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, lifetimeInSeconds);
        return calendar.getTime();
    }

    public Map<String, JSONObject> findRemovedKeys(JSONObject existingJwks, JSONObject newJwks) {
        Map<String, JSONObject> existingMap = createKidToKeyMap(existingJwks);
        Map<String, JSONObject> newMap = createKidToKeyMap(newJwks);

        for (String kid : newMap.keySet()) {
            existingMap.remove(kid);
        }

        return existingMap;
    }

    public void archiveRemovedKeys(JSONObject existingJwks, JSONObject newJwks) {
        Map<String, JSONObject> removedKeys = findRemovedKeys(existingJwks, newJwks);


        for (Map.Entry<String, JSONObject> entry : removedKeys.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            archiveJwk(entry.getValue());
        }
    }

    public static Map<String, JSONObject> createKidToKeyMap(JSONObject jwks) {
        Map<String, JSONObject> map = new HashMap<>();

        JSONArray keys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);
            final String kid = key.optString(KEY_ID);
            map.put(kid, key);
        }

        return map;
    }
}
