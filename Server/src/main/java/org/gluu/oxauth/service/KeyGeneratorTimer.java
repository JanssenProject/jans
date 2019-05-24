/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import static org.gluu.oxauth.model.jwk.JWKParameter.EXPIRATION_TIME;
import static org.gluu.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static org.gluu.oxauth.model.jwk.JWKParameter.KEY_ID;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.config.ConfigurationFactory;
import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.CryptoProviderFactory;
import org.gluu.oxauth.service.cdi.event.KeyGenerationEvent;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

/**
 * @author Javier Rojas Blum
 * @version January 1, 2019
 */
@ApplicationScoped
@Named
public class KeyGeneratorTimer {

    private final static String EVENT_TYPE = "KeyGeneratorTimerEvent";

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

    private AtomicBoolean isActive;
	private long lastFinishedTime;

    public void initTimer() {
        log.debug("Initializing Key Generator Timer");
        this.isActive = new AtomicBoolean(false);

        // Schedule to start every 1 minute
		final int delay = 1 * 60;
		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, DEFAULT_INTERVAL), new KeyGenerationEvent(),
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

	private void updateKeys() throws JSONException, Exception {
		if (!isStartUpdateKeys()) {
			return;
		}

		updateKeysImpl();
		this.lastFinishedTime = System.currentTimeMillis();
	}

	private boolean isStartUpdateKeys() {
		int poolingInterval = appConfiguration.getKeyRegenerationInterval();
        if (poolingInterval <= 0) {
        	poolingInterval = DEFAULT_INTERVAL;
        }

        poolingInterval = poolingInterval * 3600 * 1000;

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

		return timeDiffrence >= poolingInterval;
	}

    private void updateKeysImpl() throws JSONException, Exception {
        String dn = configurationFactory.getBaseConfiguration().getString("oxauth_ConfigurationEntryDN");
        Conf conf = ldapEntryManager.find(Conf.class, dn);

        JSONObject jwks = conf.getWebKeys().toJSONObject();
        JSONObject updatedJwks =  updateKeys(jwks);

        conf.setWebKeys(ServerUtil.createJsonMapper().readValue(updatedJwks.toString(), WebKeysConfiguration.class));

        long nextRevision = conf.getRevision() + 1;
        conf.setRevision(nextRevision);
        ldapEntryManager.merge(conf);
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