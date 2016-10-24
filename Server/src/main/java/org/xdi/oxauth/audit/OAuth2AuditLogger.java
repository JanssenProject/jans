package org.xdi.oxauth.audit;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.BooleanUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.util.ServerUtil;

import javax.jms.*;
import java.io.IOException;

@Name("oAuth2AuditLogger")
@Scope(ScopeType.APPLICATION)
@Startup
public class OAuth2AuditLogger {

    private final String BROKER_URL = "tcp://localhost:61616";
    private final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    private final String CLIENT_QUEUE_NAME = "oauth2.audit.logging";

    @Logger
    private Log logger;

    private MessageProducer producer;
    private Connection connection;
    private Session session;

    private boolean transacted = false;


    public OAuth2AuditLogger() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                BROKER_URL);
        try {
            this.connection = connectionFactory.createConnection();
            this.connection.start();
            this.session = connection.createSession(transacted, ACK_MODE);
            this.producer = session
                    .createProducer(session.createQueue(CLIENT_QUEUE_NAME));
        } catch (JMSException e) {
            logger.error("Can't initialize the logger", e);
        }
    }

    public void sendMessage(OAuth2AuditLog oAuth2AuditLog) {
        if( BooleanUtils.isNotTrue(ConfigurationFactory.instance().getConfiguration().getEnabledOAuthAuditLogging()))
            return;

        try {
            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText(ServerUtil.asPrettyJson(oAuth2AuditLog));
            this.producer.send(txtMessage);
        } catch (JMSException e) {
            logger.error("Can't send message", e);
        }catch (IOException e) {
            logger.error("Can't serialize the audit log", e);
        }catch (Exception e) {
            logger.error("Can't send message, please check your activeMQ configuration.", e);
        }
    }

    @Destroy
    private void onDestroy() {
        try {
            //There is no need to close the sessions, producers, and consumers of a closed connection.
            this.connection.close();
        } catch (JMSException e) {
            logger.error("Can't close connection", e);
        }
    }

}
