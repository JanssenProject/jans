/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service;

import java.util.Date;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.xdi.model.SmtpConfiguration;
import org.xdi.util.StringHelper;

/**
 * Provides operations with sending E-mails
 * 
 * @author Yuriy Movchan Date: 20/04/2014
 */
@ApplicationScoped
@Named
public class MailService {

	@Inject
	private Logger log;

	@Inject
	private SmtpConfiguration smtpConfiguration;

	private long connectionTimeout = 5000;

	public boolean sendMail(String to, String subject, String message) {
		return sendMail(smtpConfiguration, null, to, subject, message);
	}

	public boolean sendMail(String from, String to, String subject, String message) {
		return sendMail(smtpConfiguration, from, to, subject, message);
	}

	public boolean sendMail(SmtpConfiguration mailSmtpConfiguration, String from, String to, String subject, String message) {
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

			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(userName, password);
				}
			});
		} else {
			Session.getInstance(props, null);
		}

		MimeMessage msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress(mailFrom));
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(subject, "UTF-8");
			msg.setSentDate(new Date());
			msg.setText(message + "\n", "UTF-8");

			Transport.send(msg);
		} catch (MessagingException ex) {
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
