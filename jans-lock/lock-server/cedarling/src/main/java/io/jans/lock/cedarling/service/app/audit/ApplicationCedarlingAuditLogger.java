package io.jans.lock.cedarling.service.app.audit;

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ApplicationCedarlingAuditLogger {

    @Inject
    private Logger log;

    private ObjectMapper mapper;

    @PostConstruct
	public void init() {
		this.mapper = new ObjectMapper();
		this.mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
		this.mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

    public void log(AuditLogEntry auditLogEntry) {
    	if (auditLogEntry != null) {
    		loggingThroughFile(auditLogEntry);
    	}
    }

    private void loggingThroughFile(AuditLogEntry auditLogEntry) {
        try {
            if (log.isInfoEnabled()) {
                String entry = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(auditLogEntry);
                log.info(entry);
            }
        } catch (IOException e) {
            log.error("Can't serialize the application audit log", e);
        }
    }

}
