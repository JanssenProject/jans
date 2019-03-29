/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.Conf;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.service.cdi.event.KeyGenerationEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version January 1, 2019
 */
@ApplicationScoped
@Named
public class KeyGeneratorTimer {

    private final static String EVENT_TYPE = "KeyGeneratorTimerEvent";
    private final static int DEFAULT_INTERVAL = 48; // 48 hours

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private AppConfiguration appConfiguration;

    private AtomicBoolean isActive;

    public void initTimer() {
        log.debug("Initializing Key Generator Timer");

        this.isActive = new AtomicBoolean(false);

        int interval = appConfiguration.getKeyRegenerationInterval();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }

        interval = interval * 3600;
        timerEvent.fire(new TimerEvent(new TimerSchedule(interval, interval), new KeyGenerationEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void process(@Observes @Scheduled KeyGenerationEvent keyGenerationEvent) {
        if (!appConfiguration.getKeyRegenerationEnabled()) {
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
        String dn = configurationFactory.getPersistenceConfiguration().getConfiguration().getString("oxauth_ConfigurationEntryDN");
        Conf conf = ldapEntryManager.find(Conf.class, dn);

        JSONObject jwks = new JSONObject(conf.getWebKeys());
        conf.setWebKeys(updateKeys(jwks).toString());

        long nextRevision = conf.getRevision() + 1;
        conf.setRevision(nextRevision);
        ldapEntryManager.merge(conf);

        return conf.getWebKeys();
    }

    private JSONObject updateKeys(JSONObject jwks) throws Exception {
        JSONObject jsonObject = AbstractCryptoProvider.generateJwks(appConfiguration.getKeyRegenerationInterval(),
                appConfiguration.getIdTokenLifetime(), appConfiguration);

        JSONArray keys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);

            if (key.has(EXPIRATION_TIME) && !key.isNull(EXPIRATION_TIME)) {
                GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationDate.setTimeInMillis(key.getLong(EXPIRATION_TIME));

                if (expirationDate.before(now)) {
                    // The expired key is not added to the array of keys
                    log.debug("Removing JWK: {}, Expiration date: {}", key.getString(KEY_ID),
                            key.getString(EXPIRATION_TIME));
                    AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);
                    cryptoProvider.deleteKey(key.getString(KEY_ID));
                } else {
                    jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
                }
            } else {
                GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationTime.add(GregorianCalendar.HOUR, appConfiguration.getKeyRegenerationInterval());
                expirationTime.add(GregorianCalendar.SECOND, appConfiguration.getIdTokenLifetime());
                key.put(EXPIRATION_TIME, expirationTime.getTimeInMillis());

                jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
            }
        }

        return jsonObject;
    }

}