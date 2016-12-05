package org.xdi.oxauth.audit;

import com.google.common.base.Objects;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.util.ServerUtil;

import javax.jms.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Name("oAuth2AuditLogger")
@Scope(ScopeType.APPLICATION)
@Startup
public class OAuth2AuditLogger {

    private final String BROKER_URL_PREFIX = "failover:(";
    private final String BROKER_URL_SUFFIX = ")?timeout=5000&jms.useAsyncSend=true";
    private final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    private final String CLIENT_QUEUE_NAME = "oauth2.audit.logging";
    private final boolean transacted = false;

    private MessageProducer producer;
    private Connection connection;
    private Session session;

    private Set<String> jmsBrokerURISet;
    private String jmsUserName;
    private String jmsPassword;

    @Logger
    private Log logger;

	private final ReentrantLock lock = new ReentrantLock();

	@Create
    public void init() {
        tryToEstablishJMSConnection();
    }

    @Asynchronous
    public synchronized void sendMessage(OAuth2AuditLog oAuth2AuditLog) {
        if (BooleanUtils.isNotTrue(isEnabledOAuthAuditnLogging()))
            return;

        if (this.connection == null || isJmsConfigChanged()) {
            if (tryToEstablishJMSConnection())
                loggingThroughJMS(oAuth2AuditLog);
            else
                loggingThroughFile(oAuth2AuditLog);
        } else {
            loggingThroughJMS(oAuth2AuditLog);
        }
    }

    @Destroy
    public void destroy() {
        if (this.connection == null)
            return;
        try {
            // There is no need to close the sessions, producers, and consumers
            // of a closed connection.
            this.connection.close();
        } catch (JMSException e) {
            logger.error("Can't close connection", e);
        }
    }

	private boolean tryToEstablishJMSConnection() {
		lock.lock();
		try {
	        if (this.connection == null || isJmsConfigChanged()) {
				return tryToEstablishJMSConnectionImpl();
			}

	        return true;
		} finally {
			lock.unlock();
		}
	}

    private boolean tryToEstablishJMSConnectionImpl() {
        destroy();

        Set<String> jmsBrokerURISet = getJmsBrokerURISet();
        if (BooleanUtils.isNotTrue(isEnabledOAuthAuditnLogging()) || CollectionUtils.isEmpty(jmsBrokerURISet)) {
            this.producer = null;
            this.connection = null;
            this.session = null;
            return false;
        }

        this.jmsBrokerURISet = new HashSet<String>(jmsBrokerURISet);
        this.jmsUserName = getJmsUserName();
        this.jmsPassword = getJmsPassword();

        Iterator<String> jmsBrokerURIIterator = jmsBrokerURISet.iterator();

        StringBuilder uriBuilder = new StringBuilder();
        while (jmsBrokerURIIterator.hasNext()) {
            String jmsBrokerURI = jmsBrokerURIIterator.next();
            uriBuilder.append("tcp://");
            uriBuilder.append(jmsBrokerURI);
            if (jmsBrokerURIIterator.hasNext())
                uriBuilder.append(",");
        }

        String brokerUrl = BROKER_URL_PREFIX + uriBuilder + BROKER_URL_SUFFIX;

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        try {
            this.connection = connectionFactory.createConnection(this.jmsUserName, this.jmsPassword);
            this.connection.start();
            this.session = connection.createSession(transacted, ACK_MODE);
            this.producer = session.createProducer(session.createQueue(CLIENT_QUEUE_NAME));
        } catch (JMSException e) {
            logger.error("Can't establish connection to jms broker");
            return false;
        }
        return true;
    }

    private void loggingThroughJMS(OAuth2AuditLog oAuth2AuditLog) {
        try {
            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText(ServerUtil.asPrettyJson(oAuth2AuditLog));
            this.producer.send(txtMessage);
        } catch (JMSException e) {
            logger.error("Can't send message", e);
        } catch (IOException e) {
            logger.error("Can't serialize the audit log", e);
        } catch (Exception e) {
            logger.error("Can't send message, please check your activeMQ configuration.", e);
        }
    }

    private void loggingThroughFile(OAuth2AuditLog oAuth2AuditLog) {
        try {
            logger.info(ServerUtil.asPrettyJson(oAuth2AuditLog));
        } catch (IOException e) {
            logger.error("Can't serialize the audit log", e);
        }
    }

    private boolean isJmsConfigChanged() {
        return !Objects.equal(this.jmsUserName, getJmsUserName()) || !Objects.equal(this.jmsPassword, getJmsPassword())
                || !Objects.equal(this.jmsBrokerURISet, getJmsBrokerURISet());
    }

    private Boolean isEnabledOAuthAuditnLogging() {
        return ConfigurationFactory.instance().getConfiguration().getEnabledOAuthAuditLogging();
    }

    private Set<String> getJmsBrokerURISet() {
        return ConfigurationFactory.instance().getConfiguration().getJmsBrokerURISet();
    }

    private String getJmsUserName() {
        return ConfigurationFactory.instance().getConfiguration().getJmsUserName();
    }

    private String getJmsPassword() {
        return ConfigurationFactory.instance().getConfiguration().getJmsPassword();
    }
}