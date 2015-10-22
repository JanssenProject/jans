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
import org.xdi.oxauth.model.config.Configuration;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.util.KeyGenerator;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Javier Rojas Blum
 * @version 0.9 February 25, 2015
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
        //interval = 1L * 60L * 1000L;
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

        JSONArray keys = jwks.getJSONArray("keys");
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);

            if (key.has("expirationTime") && !key.isNull("expirationTime")) {
                GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationDate.setTimeInMillis(key.getLong("expirationTime"));

                if (expirationDate.before(now)) {
                    // The expired key is not added to the array of keys
                    log.debug("Removing JWK: {0}, Expiration date: {1}",
                            key.get("keyId"),
                            key.get("expirationTime"));
                } else {
                    jsonObject.getJSONArray("keys").put(key);
                }
            } else {
                GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationTime.add(GregorianCalendar.HOUR, ConfigurationFactory.instance().getConfiguration().getKeyRegenerationInterval());
                expirationTime.add(GregorianCalendar.SECOND, ConfigurationFactory.instance().getConfiguration().getIdTokenLifetime());
                key.put("expirationTime", expirationTime.getTimeInMillis());

                jsonObject.getJSONArray("keys").put(key);
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
        jsonObject.put("keys", keys);

        return jsonObject;
    }
}