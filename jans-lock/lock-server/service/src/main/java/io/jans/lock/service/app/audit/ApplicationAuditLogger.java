package io.jans.lock.service.app.audit;

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import java.io.IOException;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.app.audit.AuditLogEntry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @version November 03, 2025
 */
@ApplicationScoped
public class ApplicationAuditLogger {

	@Inject
	private Logger log;

	private ObjectMapper mapper;

	@PostConstruct
	public void init() {
		this.mapper = new ObjectMapper();
	}

	@PreDestroy
	public void destroy() {
	}

	public void log(AuditLogEntry auditLogEntry) {
		loggingThroughFile(auditLogEntry);
	}

	private void loggingThroughFile(AuditLogEntry auditLogEntry) {
		try {
			if (log.isInfoEnabled()) {
				String entry = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(auditLogEntry);
				log.info(entry);
			}
		} catch (IOException e) {
			log.error("Can't serialize the a[[lication audit log", e);
		}
	}

}
