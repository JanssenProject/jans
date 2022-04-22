/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.audit;

import com.google.common.base.Objects;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.util.ServerUtil;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.util.StringHelper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Named
@ApplicationScoped
@DependsOn("appInitializer")
public class ApplicationAuditLogger {

    private static final String BROKER_URL_PREFIX = "failover:(";
    private static final String BROKER_URL_SUFFIX = ")?timeout=5000&jms.useAsyncSend=true";
    private static final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    private static final String CLIENT_QUEUE_NAME = "oauth2.audit.logging";
    private static final boolean TRANSACTED = false;

    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    private Logger log;
    @Inject
    private AppConfiguration appConfiguration;

    private final AtomicReference<PooledConnectionFactory> pooledConnectionFactory = new AtomicReference<>();
    private Set<String> jmsBrokerURISet;
    private String jmsUserName;
    private String jmsPassword;
    private boolean enabled;
    private boolean sendAuditJms;

    @PostConstruct
    public void init() {
        updateConfiguration(appConfiguration);
    }

    public void updateConfiguration(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
        this.enabled = BooleanUtils.isTrue(appConfiguration.getEnabledOAuthAuditLogging());
        this.sendAuditJms = StringHelper.isNotEmpty(appConfiguration.getJmsUserName())
                && StringHelper.isNotEmpty(appConfiguration.getJmsPassword())
                && CollectionUtils.isNotEmpty(appConfiguration.getJmsBrokerURISet());

        boolean configChanged = !Objects.equal(this.jmsUserName, appConfiguration.getJmsUserName())
                || !Objects.equal(this.jmsPassword, appConfiguration.getJmsPassword())
                || !Objects.equal(this.jmsBrokerURISet, appConfiguration.getJmsBrokerURISet());

        if (configChanged) {
            destroy();
        }
    }

    @Asynchronous
    public void sendMessage(OAuth2AuditLog oAuth2AuditLog) {
        if (!enabled) {
            return;
        }

        boolean messageDelivered = false;
        if (sendAuditJms && tryToEstablishJMSConnection()) {
            messageDelivered = loggingThroughJMS(oAuth2AuditLog);
        }

        if (!messageDelivered) {
            loggingThroughFile(oAuth2AuditLog);
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.pooledConnectionFactory.get() == null) {
            return;
        }

        this.pooledConnectionFactory.getAndSet(null).clear();
    }

    private boolean tryToEstablishJMSConnection() {
        if (this.pooledConnectionFactory.get() != null) {
            return true;
        }

        lock.lock();
        try {
            // Check if another thread initialized JMS pool already
            if (this.pooledConnectionFactory.get() == null) {
                return tryToEstablishJMSConnectionImpl();
            }

            return true;
        } finally {
            lock.unlock();
        }
    }

    private boolean tryToEstablishJMSConnectionImpl() {
        Set<String> uriSet = appConfiguration.getJmsBrokerURISet();
        if (!enabled || CollectionUtils.isEmpty(uriSet)) {
            return false;
        }

        this.jmsBrokerURISet = new HashSet<>(uriSet);
        this.jmsUserName = appConfiguration.getJmsUserName();
        this.jmsPassword = appConfiguration.getJmsPassword();

        Iterator<String> jmsBrokerURIIterator = uriSet.iterator();

        StringBuilder uriBuilder = new StringBuilder();
        while (jmsBrokerURIIterator.hasNext()) {
            String jmsBrokerURI = jmsBrokerURIIterator.next();
            uriBuilder.append("tcp://");
            uriBuilder.append(jmsBrokerURI);
            if (jmsBrokerURIIterator.hasNext()) {
                uriBuilder.append(",");
            }
        }

        String brokerUrl = BROKER_URL_PREFIX + uriBuilder + BROKER_URL_SUFFIX;

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(this.jmsUserName, this.jmsPassword, brokerUrl);

        final PooledConnectionFactory pool = new PooledConnectionFactory(connectionFactory);
        pool.setIdleTimeout(5000);
        pool.setMaxConnections(10);
        pool.start();

        this.pooledConnectionFactory.set(pool);
        return true;
    }

    private boolean loggingThroughJMS(OAuth2AuditLog oAuth2AuditLog) {
        try (QueueConnection connection = pooledConnectionFactory.get().createQueueConnection()) {
            connection.start();

            try (QueueSession session = connection.createQueueSession(TRANSACTED, ACK_MODE);
                 MessageProducer producer = session.createProducer(session.createQueue(CLIENT_QUEUE_NAME))) {
                TextMessage txtMessage = session.createTextMessage();
                txtMessage.setText(ServerUtil.asPrettyJson(oAuth2AuditLog));
                producer.send(txtMessage);
            }

            return true;
        } catch (Exception e) {
            log.error("Can't send message, please check your activeMQ configuration.", e);
        }
        return false;
    }

    private void loggingThroughFile(OAuth2AuditLog oAuth2AuditLog) {
        try {
            if (log.isInfoEnabled()) {
                log.info(ServerUtil.asPrettyJson(oAuth2AuditLog));
            }
        } catch (IOException e) {
            log.error("Can't serialize the audit log", e);
        }
    }

}
