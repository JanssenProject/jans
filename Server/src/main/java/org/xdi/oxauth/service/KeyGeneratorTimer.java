/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.Conf;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
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

        long interval = configurationFactory.getConfiguration().getKeyRegenerationInterval();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }

        interval = interval * 3600L * 1000L;
        Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(interval, interval));
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void process() {
        if (!configurationFactory.getConfiguration().getKeyRegenerationEnabled()) {
            return;
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            updateKeys();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            this.isActive.set(false);
        }
    }

    public String updateKeys() throws JSONException, Exception {
        String dn = configurationFactory.getLdapConfiguration().getString("configurationEntryDN");
        Conf conf = ldapEntryManager.find(Conf.class, dn);

        JSONObject jwks = new JSONObject(conf.getWebKeys());
        conf.setWebKeys(updateKeys(jwks).toString());

        long nextRevision = conf.getRevision() + 1;
        conf.setRevision(nextRevision);
        ldapEntryManager.merge(conf);

        return conf.getWebKeys();
    }

    private JSONObject updateKeys(JSONObject jwks) throws Exception {
        JSONObject jsonObject = AbstractCryptoProvider.generateJwks(
                configurationFactory.getConfiguration().getKeyRegenerationInterval(),
                configurationFactory.getConfiguration().getIdTokenLifetime(),
                ConfigurationFactory.instance().getConfiguration());

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
                            key.getString(KEY_ID),
                            key.getString(EXPIRATION_TIME));
                    AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(
                            ConfigurationFactory.instance().getConfiguration());
                    cryptoProvider.deleteKey(key.getString(KEY_ID));
                } else {
                    jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
                }
            } else {
                GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationTime.add(GregorianCalendar.HOUR, configurationFactory.getConfiguration().getKeyRegenerationInterval());
                expirationTime.add(GregorianCalendar.SECOND, configurationFactory.getConfiguration().getIdTokenLifetime());
                key.put(EXPIRATION_TIME, expirationTime.getTimeInMillis());

                jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
            }
        }

        return jsonObject;
    }

    /**
     * Get KeyGeneratorTimer instance
     *
     * @return KeyGeneratorTimer instance
     */
    public static KeyGeneratorTimer instance() {
        return (KeyGeneratorTimer) Component.getInstance(KeyGeneratorTimer.class);
    }

}