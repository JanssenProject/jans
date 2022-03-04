/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import io.jans.model.SmtpConfiguration;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;

/**
 * Provides operations with sending E-mails
 *
 * @author Yuriy Movchan Date: 20/04/2014
 */
@RequestScoped
@Named
public class MailService {

    @Inject
    private Logger log;

    @Inject
    private SmtpConfiguration smtpConfiguration;

    private long connectionTimeout = 5000;

    public boolean sendMail(String to, String subject, String message) {
        return sendMail(smtpConfiguration, null, null, to, null, subject, message, null);
    }

    public boolean sendMail(String to, String toDisplayName, String subject, String message, String htmlMessage) {
        return sendMail(smtpConfiguration, null, null, to, null, subject, message, htmlMessage);
    }

    public boolean sendMail(String from, String fromDisplayName, String to, String toDisplayName, String subject,
            String message, String htmlMessage) {
        return sendMail(smtpConfiguration, from, fromDisplayName, to, null, subject, message, htmlMessage);
    }

    public boolean sendMail(SmtpConfiguration mailSmtpConfiguration, String from, String fromDisplayName, String to,
            String toDisplayName,
            String subject, String message, String htmlMessage) {
        if (mailSmtpConfiguration == null) {
            log.error("Failed to send message from '{}' to '{}' because the SMTP configuration isn't valid!", from, to);
            return false;
        }

        log.debug("Host name: " + mailSmtpConfiguration.getHost() + ", port: " + mailSmtpConfiguration.getPort() + ", connection time out: "
                + this.connectionTimeout);

        String mailFrom = from;
        if (StringHelper.isEmpty(mailFrom)) {
            mailFrom = mailSmtpConfiguration.getFromEmailAddress();
        }

        String mailFromName = fromDisplayName;
        if (StringHelper.isEmpty(mailFromName)) {
            mailFromName = mailSmtpConfiguration.getFromName();
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", mailSmtpConfiguration.getHost());
        props.put("mail.smtp.port", mailSmtpConfiguration.getPort());
        props.put("mail.from", mailFrom);
        props.put("mail.smtp.connectiontimeout", this.connectionTimeout);
        props.put("mail.smtp.timeout", this.connectionTimeout);
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.ssl.trust", mailSmtpConfiguration.getHost());

        if (mailSmtpConfiguration.isRequiresSsl()) {
            props.put("mail.smtp.socketFactory.port", mailSmtpConfiguration.getPort());
            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        Session session = null;
        if (mailSmtpConfiguration.isRequiresAuthentication()) {
            props.put("mail.smtp.auth", "true");

            final String userName = mailSmtpConfiguration.getUserName();
            final String password = mailSmtpConfiguration.getPasswordDecrypted();

            session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
        } else {
            session = Session.getInstance(props, null);
        }

        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(mailFrom, mailFromName));
            if (StringHelper.isEmpty(toDisplayName)) {
                msg.setRecipients(Message.RecipientType.TO, to);
            } else {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, toDisplayName));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());

            if (StringHelper.isEmpty(htmlMessage)) {
                msg.setText(message + "\n", "UTF-8", "plain");
            } else {
                // Unformatted text version
                final MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(message, "UTF-8", "plain");
                // HTML version
                final MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setText(htmlMessage, "UTF-8", "html");

                // Create the Multipart. Add BodyParts to it.
                final Multipart mp = new MimeMultipart("alternative");
                mp.addBodyPart(textPart);
                mp.addBodyPart(htmlPart);

                // Set Multipart as the message's content
                msg.setContent(mp);
            }

            Transport.send(msg);
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            return false;
        }

        return true;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

}
