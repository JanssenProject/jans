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

    private void procesMetadataValidation() throws Exception {
        log.debug("Starting metadata validation");
        boolean result = validateMetadata(samlIdpService.getIdpMetadataTempDir(), samlIdpService.getIdpMetadataDir());
        log.debug("Metadata validation finished with result: '{}'", result);

    }

    public void queue(String fileName) {
        synchronized (metadataUpdates) {
            log.debug("fileNamem:{}, metadataUpdates.contains(fileName):{}",fileName, metadataUpdates.contains(fileName));
            if(!metadataUpdates.contains(fileName)) {
                metadataUpdates.add(fileName);
            }
        }
    }

    public boolean isQueued(String samlspMetaDataFN) {
        synchronized (metadataUpdates) {
            for (String filename : metadataUpdates) {
                if (filename.contains(samlspMetaDataFN)) {
                    return true;
                }
            }
            return false;
        }
    }

    public String getValidationStatus(String samlspMetaDataFN, TrustRelationship trust) {
        if (trust.getValidationStatus() == null) {
            return ValidationStatus.SUCCESS.getDisplayName();
        }
        if (trust.getValidationStatus() == null) {
            return ValidationStatus.PENDING.getDisplayName();
        }
        synchronized (metadataUpdates) {
            boolean result = false;
            for (String filename : metadataUpdates) {
                if (filename.contains(samlspMetaDataFN)) {
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

    /**
     * @param tempmetadataFolder
     * @param metadataFolder
     */
    private boolean validateMetadata(String tempmetadataFolder, String metadataFolder) throws Exception {
        boolean result = false;
        log.error("Starting metadata validation process.");

        String metadataFN = null;
        synchronized (metadataUpdates) {
            if (!metadataUpdates.isEmpty()) {
                metadataFN = metadataUpdates.poll();
       
            }
        }
        log.error("metadataFN:{}", metadataFN);

        synchronized (this) {
            if (StringHelper.isNotEmpty(metadataFN)) {
                String metadataPath = tempmetadataFolder + metadataFN;
                String destinationMetadataName = metadataFN.replaceAll(".{4}\\..{4}$", "");
                String destinationMetadataPath = metadataFolder + destinationMetadataName;
                log.error("metadataFN:{}, metadataPath:{}, destinationMetadataName:{}, destinationMetadataPath:{}", metadataFN, metadataPath, destinationMetadataName, destinationMetadataPath);
                TrustRelationship tr = samlService
                        .getTrustByUnpunctuatedInum(metadataFN.split("-" + samlIdpService.getSpMetadataFile())[0]);
                if (tr == null) {
                    metadataUpdates.add(metadataFN);
                    return false;
                }
                tr.setValidationStatus(ValidationStatus.PENDING);
                samlService.updateTrustRelationship(tr);

                log.error("metadataFN:{}, metadataPath:{}, destinationMetadataName:{}, destinationMetadataPath:{}", metadataFN, metadataPath, destinationMetadataName, destinationMetadataPath);
                
                GluuErrorHandler errorHandler = null;
                List<String> validationLog = null;
                try {
                    errorHandler = samlIdpService.validateMetadata(metadataPath);
                    log.error("validateMetadata result errorHandler:{}", errorHandler);
                } catch (Exception e) {
                    tr.setValidationStatus(ValidationStatus.FAILED);
                    tr.setStatus(GluuStatus.INACTIVE);
                    validationLog = new ArrayList<String>();
                    validationLog.add(e.getMessage());
                    log.error("Validation of " + tr.getInum() + " failed: " + e.getMessage());
                    tr.setValidationLog(validationLog);
                    samlService.updateTrustRelationship(tr);
                    return false;
                }
                
                if(errorHandler==null) {
                    return false;
                }
                log.error("validateMetadata result errorHandler.isValid():{}, errorHandler.getLog():{}, errorHandler.toString():{}", errorHandler.isValid(), errorHandler.getLog(), errorHandler.toString());
                log.error("samlAppConfiguration.isIgnoreValidation():{} errorHandler.isInternalError():{}", samlAppConfiguration.isIgnoreValidation(), errorHandler.isInternalError());
                
                if (errorHandler.isValid()) {
                    log.error("validate Metadata file processing");
                    tr.setValidationLog(errorHandler.getLog());
                    tr.setValidationStatus(ValidationStatus.SUCCESS);

                    log.error("Move metadata file:{} to location:{}", metadataPath, destinationMetadataPath);
                    boolean renamed = samlIdpService.renameMetadata(metadataPath, destinationMetadataPath);
                    
                    log.error("Staus of moving file:{} to location:{} is :{}", metadataPath, destinationMetadataPath, renamed);
                    
                    if (renamed) {
                        log.error("Failed to move metadata file:{} to location:{}", metadataPath, destinationMetadataPath);
                        tr.setStatus(GluuStatus.INACTIVE);
                    } else {
                        tr.setSpMetaDataFN(destinationMetadataName);
                    }

                    String metadataFile = samlIdpService.getIdpMetadataDir() + tr.getSpMetaDataFN();
                    log.error("After successfully moving metadataFile :{}",metadataFile);
                    List<String> entityIdList = samlMetadataParser.getEntityIdFromMetadataFile(metadataFile);
                    log.error("Success entityIdList :{}",entityIdList);
                    Set<String> entityIdSet = new TreeSet<String>();
                    Set<String> duplicatesSet = new TreeSet<String>();
                    if (entityIdList != null && !entityIdList.isEmpty()) {

                        for (String entityId : entityIdList) {
                            if (!entityIdSet.add(entityId)) {
                                duplicatesSet.add(entityId);
                            }
                        }
                    }
                    log.error("Success duplicatesSet :{}",duplicatesSet);
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
                        log.error("Failed validation for destinationMetadataPath:{}" + destinationMetadataPath);
                    }

                    String metadataFile = samlIdpService.getIdpMetadataDir() + tr.getSpMetaDataFN();
                    log.error("Failed status metadataFile :{}",metadataFile);
                    
                    List<String> entityIdList = samlMetadataParser.getEntityIdFromMetadataFile(metadataFile);
                    log.error("Failed status entityIdList :{}",entityIdList);
                    Set<String> duplicatesSet = new TreeSet<String>();
                    Set<String> entityIdSet = new TreeSet<String>();

                    for (String entityId : entityIdList) {
                        if (!entityIdSet.add(entityId)) {
                            duplicatesSet.add(entityId);
                        }
                    }

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
                    log.error("Failed status validationLog :{}",validationLog);
                    samlService.updateTrustRelationship(tr);
                    result = true;
                } else {
                    log.error("Unhandled  metadataFN:{}",metadataFN);
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
