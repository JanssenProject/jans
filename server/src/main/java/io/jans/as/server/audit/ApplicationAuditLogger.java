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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Named
@ApplicationScoped
@DependsOn("appInitializer")
public class ApplicationAuditLogger {

    private final String BROKER_URL_PREFIX = "failover:(";
    private final String BROKER_URL_SUFFIX = ")?timeout=5000&jms.useAsyncSend=true";
    private final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    private final String CLIENT_QUEUE_NAME = "oauth2.audit.logging";
    private final boolean transacted = false;

    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    private Logger log;
    @Inject
    private AppConfiguration appConfiguration;

    private volatile PooledConnectionFactory pooledConnectionFactory;
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
        if (sendAuditJms) {
            if (tryToEstablishJMSConnection()) {
                messageDelivered = loggingThroughJMS(oAuth2AuditLog);
            }
        }

        if (!messageDelivered) {
            loggingThroughFile(oAuth2AuditLog);
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.pooledConnectionFactory == null) {
            return;
        }

        this.pooledConnectionFactory.clear();
        this.pooledConnectionFactory = null;
    }

    private boolean tryToEstablishJMSConnection() {
        if (this.pooledConnectionFactory != null) {
            return true;
        }

        lock.lock();
        try {
            // Check if another thread initialized JMS pool already
            if (this.pooledConnectionFactory == null) {
                return tryToEstablishJMSConnectionImpl();
            }

            return true;
        } finally {
            lock.unlock();
        }
    }

    private boolean tryToEstablishJMSConnectionImpl() {
        Set<String> jmsBrokerURISet = appConfiguration.getJmsBrokerURISet();
        if (!enabled || CollectionUtils.isEmpty(jmsBrokerURISet)) {
            return false;
        }

        this.jmsBrokerURISet = new HashSet<>(jmsBrokerURISet);
        this.jmsUserName = appConfiguration.getJmsUserName();
        this.jmsPassword = appConfiguration.getJmsPassword();

        Iterator<String> jmsBrokerURIIterator = jmsBrokerURISet.iterator();

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
        this.pooledConnectionFactory = new PooledConnectionFactory(connectionFactory);

        pooledConnectionFactory.setIdleTimeout(5000);
        pooledConnectionFactory.setMaxConnections(10);
        pooledConnectionFactory.start();

        return true;
    }

    private boolean loggingThroughJMS(OAuth2AuditLog oAuth2AuditLog) {
        QueueConnection connection = null;
        try {
            connection = pooledConnectionFactory.createQueueConnection();
            connection.start();

            QueueSession session = connection.createQueueSession(transacted, ACK_MODE);
            MessageProducer producer = session.createProducer(session.createQueue(CLIENT_QUEUE_NAME));

            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText(ServerUtil.asPrettyJson(oAuth2AuditLog));
            producer.send(txtMessage);

            return true;
        } catch (JMSException e) {
            log.error("Can't send message", e);
        } catch (IOException e) {
            log.error("Can't serialize the audit log", e);
        } catch (Exception e) {
            log.error("Can't send message, please check your activeMQ configuration.", e);
        } finally {
            if (connection == null) {
                return false;
            }

            try {
                connection.close();
            } catch (JMSException e) {
                log.error("Can't close connection.");
            }
        }

        return false;
    }

    private void loggingThroughFile(OAuth2AuditLog oAuth2AuditLog) {
        try {
            log.info(ServerUtil.asPrettyJson(oAuth2AuditLog));
        } catch (IOException e) {
            log.error("Can't serialize the audit log", e);
        }
    }

}
