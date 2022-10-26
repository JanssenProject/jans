/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.config.Conf;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.cdi.event.KeyGenerationEvent;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.jans.as.model.jwk.JWKParameter.EXPIRATION_TIME;
import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static io.jans.as.model.jwk.JWKParameter.KEY_ID;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Named
public class KeyGeneratorTimer {

    private static final int DEFAULT_INTERVAL = 60;

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

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    private AtomicBoolean isActive;
    private long lastFinishedTime;

    public long getLastFinishedTime() {
        return lastFinishedTime;
    }

    public void initTimer() {
        log.debug("Initializing Key Generator Timer");
        this.isActive = new AtomicBoolean(false);

        timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new KeyGenerationEvent(),
                Scheduled.Literal.INSTANCE));

        this.lastFinishedTime = System.currentTimeMillis();
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
        } catch (Exception ex) {
            log.error("Exception happened while executing keys update", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    private void updateKeys() throws Exception {
        if (!isStartUpdateKeys()) {
            return;
        }

        updateKeysImpl();
        this.lastFinishedTime = System.currentTimeMillis();
    }

    private boolean isStartUpdateKeys() {
        long poolingInterval = appConfiguration.getKeyRegenerationInterval();
        if (poolingInterval <= 0) {
            poolingInterval = DEFAULT_INTERVAL;
        }

        poolingInterval = poolingInterval * 3600 * 1000L;

        long timeDifference = System.currentTimeMillis() - this.lastFinishedTime;

        return timeDifference >= poolingInterval;
    }

    private void updateKeysImpl() throws Exception {
        log.info("Updating JWKS keys ...");
        String dn = configurationFactory.getBaseConfiguration().getString(Constants.SERVER_KEY_OF_CONFIGURATION_ENTRY);
        Conf conf = ldapEntryManager.find(Conf.class, dn);

        JSONObject jwks = conf.getWebKeys().toJSONObject();
        JSONObject updatedJwks = updateKeys(jwks);

        conf.setWebKeys(ServerUtil.createJsonMapper().readValue(updatedJwks.toString(), WebKeysConfiguration.class));

        long nextRevision = conf.getRevision() + 1;
        conf.setRevision(nextRevision);
        ldapEntryManager.merge(conf);

        log.info("Updated JWKS successfully");
        log.trace("JWKS keys: " + conf.getWebKeys().getKeys().stream().map(JSONWebKey::getKid).collect(Collectors.toList()));
        log.trace("KeyStore keys: " + cryptoProvider.getKeys());
    }

    private JSONObject updateKeys(JSONObject jwks) throws Exception {
        JSONObject jsonObject = AbstractCryptoProvider.generateJwks(cryptoProvider, appConfiguration);

        JSONArray keys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);

            if (key.has(EXPIRATION_TIME) && !key.isNull(EXPIRATION_TIME)) {
                GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationDate.setTimeInMillis(key.getLong(EXPIRATION_TIME));

                if (expirationDate.before(now)) {
                    // The expired key is not added to the array of keys
                    log.trace("Removing JWK: {}, Expiration date: {}", key.getString(KEY_ID),
                            key.getLong(EXPIRATION_TIME));
                    cryptoProvider.deleteKey(key.getString(KEY_ID));
                } else if (cryptoProvider.containsKey(key.getString(KEY_ID))) {
                    log.trace("Contains kid: {}", key.getString(KEY_ID));
                    jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
                }
            } else if (cryptoProvider.containsKey(key.getString(KEY_ID))) {
                GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                expirationTime.add(GregorianCalendar.HOUR, appConfiguration.getKeyRegenerationInterval());
                expirationTime.add(GregorianCalendar.SECOND, appConfiguration.getIdTokenLifetime());
                key.put(EXPIRATION_TIME, expirationTime.getTimeInMillis());

                log.trace("Contains kid {} without exp {}", key.getString(KEY_ID), expirationTime);

                jsonObject.getJSONArray(JSON_WEB_KEY_SET).put(key);
            }
        }

        return jsonObject;
    }

}