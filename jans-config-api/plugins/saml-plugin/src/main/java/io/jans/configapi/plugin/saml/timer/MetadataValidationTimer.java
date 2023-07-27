/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.timer;


import io.jans.configapi.plugin.saml.event.MetadataValidationEvent;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.model.ValidationStatus;
import io.jans.configapi.plugin.saml.service.SamlService;
import io.jans.configapi.plugin.saml.service.SamlIdpService;
import io.jans.model.GluuStatus;
import io.jans.saml.metadata.SAMLMetadataParser;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;

import io.jans.util.StringHelper;
import io.jans.xml.GluuErrorHandler;



import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;


@ApplicationScoped
@Named
public class MetadataValidationTimer {

    private final static int DEFAULT_INTERVAL = 60; // 60 seconds

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private SamlAppConfiguration samlAppConfiguration;

    @Inject
    private SamlService samlService;

    @Inject
    private SAMLMetadataParser samlMetadataParser;

    @Inject
    private SamlIdpService samlIdpService;

    private AtomicBoolean isActive;

    private LinkedBlockingQueue<String> metadataUpdates;

    
    @PostConstruct
    public void init() {
        this.isActive = new AtomicBoolean(true);
        try {
            this.metadataUpdates = new LinkedBlockingQueue<String>();
        } finally {
            this.isActive.set(false);
        }
    }

    public void initTimer() {
        log.debug("Initializing Metadata Validation Timer");

        final int delay = 30;
        final int interval = DEFAULT_INTERVAL;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new MetadataValidationEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void processMetadataValidationTimerEvent(
            @Observes @Scheduled MetadataValidationEvent metadataValidationEvent) {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            procesMetadataValidation();
        } catch (Throwable ex) {
            log.error("Exception happened while reloading application configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    private void procesMetadataValidation() {
        log.debug("Starting metadata validation");
        boolean result = validateMetadata(samlIdpService.getIdpMetadataTempDir(),
                samlIdpService.getIdpMetadataDir());
        log.debug("Metadata validation finished with result: '{}'", result);

        if (result) {
            regenerateConfigurationFiles();
        }
    }

    public void queue(String fileName) {
        synchronized (metadataUpdates) {
            metadataUpdates.add(fileName);
        }
    }

    public boolean isQueued(String gluuSAMLspMetaDataFN) {
        synchronized (metadataUpdates) {
            for (String filename : metadataUpdates) {
                if (filename.contains(gluuSAMLspMetaDataFN)) {
                    return true;
                }
            }
            return false;
        }
    }

    public String getValidationStatus(String gluuSAMLspMetaDataFN, TrustRelationship trust) {
        if (trust.getValidationStatus() == null && trust.getGluuContainerFederation() != null) {
            return ValidationStatus.SUCCESS.getDisplayName();
        }
        if (trust.getValidationStatus() == null) {
            return ValidationStatus.PENDING.getDisplayName();
        }
        synchronized (metadataUpdates) {
            boolean result = false;
            for (String filename : metadataUpdates) {
                if (filename.contains(gluuSAMLspMetaDataFN)) {
                    result = true;
                    break;
                }
            }
            if (result) {
                return ValidationStatus.SCHEDULED.getDisplayName();
            } else {
                return trust.getValidationStatus().getDisplayName();
            }
        }
    }

    private void regenerateConfigurationFiles() {
        boolean createConfig = samlAppConfiguration.isConfigGeneration();
        if (createConfig) {
            List<TrustRelationship> trustRelationships = samlService.getAllActiveTrustRelationships();
            //samlIdpService.generateConfigurationFiles(trustRelationships);

            log.info("IDP config generation files finished. TR count: '{}'", trustRelationships.size());
        }
    }

    /**
     * @param shib3IdpTempmetadataFolder
     * @param shib3IdpMetadataFolder
     */
    private boolean validateMetadata(String shib3IdpTempmetadataFolder, String shib3IdpMetadataFolder) {
        boolean result = false;
        log.trace("Starting metadata validation process.");

        String metadataFN = null;
        synchronized (metadataUpdates) {
            if (!metadataUpdates.isEmpty()) {
                metadataFN = metadataUpdates.poll();
            }
        }

        synchronized (this) {
            if (StringHelper.isNotEmpty(metadataFN)) {
                String metadataPath = shib3IdpTempmetadataFolder + metadataFN;
                String destinationMetadataName = metadataFN.replaceAll(".{4}\\..{4}$", "");
                String destinationMetadataPath = shib3IdpMetadataFolder + destinationMetadataName;

                TrustRelationship tr = samlService.getTrustByUnpunctuatedInum(
                        metadataFN.split("-" + samlIdpService.getSpMetadataFile())[0]);
                if (tr == null) {
                    metadataUpdates.add(metadataFN);
                    return false;
                }
                tr.setValidationStatus(ValidationStatus.PENDING);
                samlService.updateTrustRelationship(tr);

                GluuErrorHandler errorHandler = null;
                List<String> validationLog = null;
                try {
                    errorHandler = samlIdpService.validateMetadata(metadataPath);
                } catch (Exception e) {
                    tr.setValidationStatus(ValidationStatus.FAILED);
                    tr.setStatus(GluuStatus.INACTIVE);
                    validationLog = new ArrayList<String>();
                    validationLog.add(e.getMessage());
                    log.warn("Validation of " + tr.getInum() + " failed: " + e.getMessage());
                    tr.setValidationLog(validationLog);
                    samlService.updateTrustRelationship(tr);

                    return false;
                }
                if (errorHandler.isValid()) {
                    tr.setValidationLog(errorHandler.getLog());
                    tr.setValidationStatus(ValidationStatus.SUCCESS);
                    /*
                     * if (samlIdpService.renameMetadata(metadataPath, destinationMetadataPath)) {
                     * log.error("Failed to move metadata file to location:" +
                     * destinationMetadataPath); tr.setStatus(GluuStatus.INACTIVE); } else {
                     * tr.setSpMetaDataFN(destinationMetadataName); }
                     */
                   // boolean federation = samlIdpService.isFederation(tr);
                   // tr.setFederation(federation);
                    String metadataFile = samlIdpService.getIdpMetadataDir() + tr.getSpMetaDataFN();

                    //List<String> entityIdList = samlMetadataParser.getEntityIdFromMetadataFile(metadataFile);
                    List<String> entityIdList = null;
                    Set<String> entityIdSet = new TreeSet<String>();
                    Set<String> duplicatesSet = new TreeSet<String>();
                    if (entityIdList != null && !entityIdList.isEmpty()) {

                        for (String entityId : entityIdList) {
                            if (!entityIdSet.add(entityId)) {
                                duplicatesSet.add(entityId);
                            }
                        }
                    }

                    if (!duplicatesSet.isEmpty()) {
                        validationLog = tr.getValidationLog();
                        if (validationLog != null) {
                            validationLog = new LinkedList<String>(validationLog);
                        } else {
                            validationLog = new LinkedList<String>();
                        }
                        validationLog.add("This metadata contains multiple instances of entityId: "
                                + Arrays.toString(duplicatesSet.toArray()));
                    }
                    tr.setValidationLog(validationLog);
                   //tr.setUniqueGluuEntityId(entityIdSet);
                    tr.setStatus(GluuStatus.ACTIVE);

                    samlService.updateTrustRelationship(tr);
                    result = true;
                } else if (samlAppConfiguration.isIgnoreValidation() || errorHandler.isInternalError()) {
                    tr.setValidationLog(new ArrayList<String>(new HashSet<String>(errorHandler.getLog())));
                    tr.setValidationStatus(ValidationStatus.FAILED);
                    if (samlIdpService.renameMetadata(metadataPath, destinationMetadataPath)) {
                        log.error("Failed to move metadata file to location:" + destinationMetadataPath);
                        tr.setStatus(GluuStatus.INACTIVE);
                    } else {
                        tr.setSpMetaDataFN(destinationMetadataName);
                    }
                   // boolean federation = samlIdpService.isFederation(tr);
                    //tr.setFederation(federation);
                    String metadataFile = samlIdpService.getIdpMetadataDir() + tr.getSpMetaDataFN();

                    List<String> entityIdList = samlMetadataParser.getEntityIdFromMetadataFile(metadataFile);
                    Set<String> duplicatesSet = new TreeSet<String>();
                    Set<String> entityIdSet = new TreeSet<String>();

                    for (String entityId : entityIdList) {
                        if (!entityIdSet.add(entityId)) {
                            duplicatesSet.add(entityId);
                        }
                    }

                  //  tr.setUniqueGluuEntityId(entityIdSet);
                    tr.setStatus(GluuStatus.ACTIVE);
                    validationLog = tr.getValidationLog();
                    if (!duplicatesSet.isEmpty()) {
                        validationLog.add("This metadata contains multiple instances of entityId: "
                                + Arrays.toString(duplicatesSet.toArray()));
                    }

                    if (errorHandler.isInternalError()) {
                        validationLog = tr.getValidationLog();

                        validationLog.add(
                                "Warning: cannot validate metadata. Check internet connetion ans www.w3.org availability.");

                        // update log with warning
                        for (String warningLogMessage : errorHandler.getLog())
                            validationLog.add("Warning: " + warningLogMessage);
                    }

                    samlService.updateTrustRelationship(tr);
                    result = true;
                } else {
                    tr.setValidationLog(new ArrayList<String>(new HashSet<String>(errorHandler.getLog())));
                    tr.setValidationStatus(ValidationStatus.FAILED);
                    tr.setStatus(GluuStatus.INACTIVE);
                    samlService.updateTrustRelationship(tr);
                }
            }
        }

        return result;
    }

}
