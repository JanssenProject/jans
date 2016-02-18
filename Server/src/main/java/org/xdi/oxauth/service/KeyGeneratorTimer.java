package org.xdi.oxauth.service;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.Conf;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.util.KeyGenerator;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */
@Name("keyGeneratorTimer")
@AutoCreate
@Scope(ScopeType.APPLICATION)
public class KeyGeneratorTimer {

    private final static String EVENT_TYPE = "KeyGeneratorTimerEvent";
    private final static int DEFAULT_INTERVAL = 48; // 48 hours

    @Logger
    private Log log;

    @In
    private ConfigurationFactory configurationFactory;

    @In
    private LdapEntryManager ldapEntryManager;

    private AtomicBoolean isActive;

    @Observer("org.jboss.seam.postInitialization")
    public void init() {
        log.debug("Initializing KeyGeneratorTimer");
        this.isActive = new AtomicBoolean(false);

        long interval = ConfigurationFactory.instance().getConfiguration().getKeyRegenerationInterval();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }

        interval = interval * 3600L * 1000L;
        Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(interval, interval));
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void process() {
        if (!ConfigurationFactory.instance().getConfiguration().getKeyRegenerationEnabled()) {
            return;
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            String dn = ConfigurationFactory.instance().getLdapConfiguration().getString("configurationEntryDN");
            Conf conf = ldapEntryManager.find(Conf.class, dn);

            long nextRevision = conf.getRevision() + 1;
            JSONObject jwks = new JSONObject(conf.getWebKeys());
            conf.setWebKeys(updateKeys(jwks).toString());

            conf.setRevision(nextRevision);
            ldapEntryManager.merge(conf);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            this.isActive.set(false);
        }
    }

    private JSONObject updateKeys(JSONObject jwks) throws Exception {
        JSONObject jsonObject = generateJwks();

        JSONArray keys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);

            if (key.has(EXPIRATION_TIME) && !key.isNull(EXPIRATION_TIME)) {
                GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationDate.setTimeInMillis(key.getLong(EXPIRATION_TIME));

                if (expirationDate.before(now)) {
                    // The expired key is not added to the array of keys
                    log.debug("Removing JWK: {0}, Expiration date: {1}",
                            key.get(KEY_ID),
                            key.get(EXPIRATION_TIME));
                } else {
                    jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
                }
            } else {
                GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationTime.add(GregorianCalendar.HOUR, ConfigurationFactory.instance().getConfiguration().getKeyRegenerationInterval());
                expirationTime.add(GregorianCalendar.SECOND, ConfigurationFactory.instance().getConfiguration().getIdTokenLifetime());
                key.put(EXPIRATION_TIME, expirationTime.getTimeInMillis());

                jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
            }
        }

        return jsonObject;
    }

    private JSONObject generateJwks() throws Exception {
        JSONArray keys = new JSONArray();

        GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expirationTime.add(GregorianCalendar.HOUR, ConfigurationFactory.instance().getConfiguration().getKeyRegenerationInterval());
        expirationTime.add(GregorianCalendar.SECOND, ConfigurationFactory.instance().getConfiguration().getIdTokenLifetime());

        keys.put(KeyGenerator.generateRS256Keys(expirationTime.getTimeInMillis()));
        keys.put(KeyGenerator.generateRS384Keys(expirationTime.getTimeInMillis()));
        keys.put(KeyGenerator.generateRS512Keys(expirationTime.getTimeInMillis()));

        keys.put(KeyGenerator.generateES256Keys(expirationTime.getTimeInMillis()));
        keys.put(KeyGenerator.generateES384Keys(expirationTime.getTimeInMillis()));
        keys.put(KeyGenerator.generateES512Keys(expirationTime.getTimeInMillis()));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_WEB_KEY_SET, keys);

        return jsonObject;
    }
}