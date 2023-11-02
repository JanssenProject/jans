/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.timer;

import io.jans.configapi.core.model.ValidationStatus;
import io.jans.configapi.plugin.keycloak.idp.broker.event.SpMetadataValidationEvent;
import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpAppConfiguration;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpService;
import io.jans.configapi.plugin.keycloak.idp.broker.service.SamlService;

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
public class SpMetadataValidationTimer {

    private final static int DEFAULT_INTERVAL = 60; // 60 seconds

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private SAMLMetadataParser samlMetadataParser;
    
    @Inject 
    SamlService samlService;
    
    @Inject
    private IdpConfigService idpConfigService;
    
    @Inject 
    private IdpService idpService;
    
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

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new SpMetadataValidationEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void processMetadataValidationTimerEvent(
            @Observes @Scheduled SpMetadataValidationEvent SpMetadataValidationEvent) {
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
        boolean result = validateMetadata(getIdpAppConfiguration().getIdpMetadataTempDir(), getIdpAppConfiguration().getSpMetadataRootDir());
        log.debug("Metadata validation finished with result: '{}'", result);

    }

    public void queue(String fileName) {
        synchronized (metadataUpdates) {
            log.debug("fileNamem:{}, metadataUpdates.contains(fileName):{}", fileName,
                    metadataUpdates.contains(fileName));
            if (!metadataUpdates.contains(fileName)) {
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

    public String getValidationStatus(String spMetaDataFN, IdentityProvider trustedIdp) {
        if (trustedIdp.getValidationStatus() == null) {
            return ValidationStatus.SUCCESS.getDisplayName();
        }
        if (trustedIdp.getValidationStatus() == null) {
            return ValidationStatus.PENDING.getDisplayName();
        }
        synchronized (metadataUpdates) {
            boolean result = false;
            for (String filename : metadataUpdates) {
                if (filename.contains(spMetaDataFN)) {
                    result = true;
                    break;
                }
            }
            if (result) {
                return ValidationStatus.SCHEDULED.getDisplayName();
            } else {
                return trustedIdp.getValidationStatus().getDisplayName();
            }
        }
    }

    /**
     * @param tempmetadataFolder
     * @param metadataFolder
     */
    private boolean validateMetadata(String tempmetadataFolder, String metadataFolder) throws Exception {
        boolean result = false;
        log.debug("Starting metadata validation process.");

        String metadataFN = null;
        synchronized (metadataUpdates) {
            if (!metadataUpdates.isEmpty()) {
                metadataFN = metadataUpdates.poll();

            }
        }
        log.debug("metadataFN:{}", metadataFN);

        synchronized (this) {
            if (metadataFN!=null && StringHelper.isNotEmpty(metadataFN)) {
                String metadataPath = tempmetadataFolder + metadataFN;
                String destinationMetadataName = metadataFN.replaceAll(".{4}\\..{4}$", "");
                String destinationMetadataPath = metadataFolder + destinationMetadataName;
                log.debug("metadataFN:{}, metadataPath:{}, destinationMetadataName:{}, destinationMetadataPath:{}",
                        metadataFN, metadataPath, destinationMetadataName, destinationMetadataPath);
                
                //To-do - start
                IdentityProvider idp = null; 
                
                
                //IdentityProvider idp = idpService
                  //      .getIdpByUnpunctuatedInum(metadataFN.split("-" + samlIdpService.getSpMetadataFile())[0]);
                
                //To-do - end
                
                log.debug("IdentityProvider found with name:{} is:{}",metadataFN, idp);
                if (idp == null) {
                    log.debug("No IdentityProvider found with name:{}",metadataFN);
                    metadataUpdates.add(metadataFN);
                    return false;
                }
                idp.setValidationStatus(ValidationStatus.PENDING);
                
                //to-do
                //idpService.updateIdp(idp);

                log.debug("metadataFN:{}, metadataPath:{}, destinationMetadataName:{}, destinationMetadataPath:{}",
                        metadataFN, metadataPath, destinationMetadataName, destinationMetadataPath);

                GluuErrorHandler errorHandler = null;
                List<String> validationLog = null;
                try {//to-do
                    //errorHandler = idpService.validateMetadata(metadataPath);
                    log.debug("validateMetadata result errorHandler:{}", errorHandler);
                } catch (Exception e) {
                    idp.setValidationStatus(ValidationStatus.FAILED);
                    idp.setStatus(GluuStatus.INACTIVE);
                    validationLog = this.getValidationLog(validationLog);
                    validationLog.add(e.getMessage());
                    log.debug("Validation of " + idp.getInum() + " failed: " + e.getMessage());
                    idp.setValidationLog(validationLog);
                  //to-do
                    //idpService.updateIdentityProvider(idp);
                    return false;
                }

                if (errorHandler == null) {
                    return false;
                }

                log.debug(
                        "validateMetadata result errorHandler.isValid():{}, errorHandler.getLog():{}, errorHandler.toString():{}",
                        errorHandler.isValid(), errorHandler.getLog(), errorHandler.toString());
                log.debug("samlAppConfiguration.isIgnoreValidation():{} errorHandler.isInternalError():{}",
                        getIdpAppConfiguration().isIgnoreValidation(), errorHandler.isInternalError());
                

                if (errorHandler.isValid()) {
                    log.debug("validate Metadata file processing");
                    idp.setValidationLog(errorHandler.getLog());
                    idp.setValidationStatus(ValidationStatus.SUCCESS);

                    log.debug("Move metadata file:{} to location:{}", metadataPath, destinationMetadataPath);
                    boolean renamed = samlService.renameMetadata(metadataPath, destinationMetadataPath);

                    log.debug("Staus of moving file:{} to location:{} is :{}", metadataPath, destinationMetadataPath,
                            renamed);

                    if (renamed) {
                        log.debug("Failed to move metadata file:{} to location:{}", metadataPath,
                                destinationMetadataPath);
                        idp.setStatus(GluuStatus.INACTIVE);
                    } else {
                        idp.setSpMetaDataFN(destinationMetadataName);
                    }

                    String metadataFile = getIdpAppConfiguration().getSpMetadataRootDir() + idp.getSpMetaDataFN();
                    log.debug("After successfully moving metadataFile :{}", metadataFile);
                    List<String> entityIdList = samlMetadataParser.getEntityIdFromMetadataFile(metadataFile);
                    log.debug("Success entityIdList :{}", entityIdList);
                    Set<String> entityIdSet = new TreeSet<String>();
                    Set<String> duplicatesSet = new TreeSet<String>();
                    if (entityIdList != null && !entityIdList.isEmpty()) {

                        for (String entityId : entityIdList) {
                            if (!entityIdSet.add(entityId)) {
                                duplicatesSet.add(entityId);
                            }
                        }
                    }
                    log.debug("Success duplicatesSet :{}", duplicatesSet);
                    if (!duplicatesSet.isEmpty()) {
                        validationLog = idp.getValidationLog();
                        if (validationLog != null) {
                            validationLog = new LinkedList<String>(validationLog);
                        } else {
                            validationLog = new LinkedList<String>();
                        }
                        validationLog.add("This metadata contains multiple instances of entityId: "
                                + Arrays.toString(duplicatesSet.toArray()));
                    }
                    idp.setValidationLog(validationLog);
                    idp.setStatus(GluuStatus.ACTIVE);

                    idpService.updateIdentityProvider(idp);
                    result = true;
                } else if (getIdpAppConfiguration().isIgnoreValidation() || errorHandler.isInternalError()) {
                    idp.setValidationLog(new ArrayList<String>(new HashSet<String>(errorHandler.getLog())));
                    idp.setValidationStatus(ValidationStatus.FAILED);
                    boolean fileRenamed = samlService.renameMetadata(metadataPath, destinationMetadataPath);
                    log.debug("Status of IdentityProvider updated to Failed, File copy from:{} to:{}, status:()",
                            metadataPath, destinationMetadataPath, fileRenamed);
                    
                    if (!fileRenamed) {
                        log.debug(
                                "Updating IdentityProvider status to Inactive as metadata:{} could not be copied to:{}",
                                metadataPath, destinationMetadataPath);
                        idp.setStatus(GluuStatus.INACTIVE);
                    } else {
                        idp.setSpMetaDataFN(destinationMetadataName);
                        log.debug("Validation error for metadata file ignored as isIgnoreValidation:{}"
                                + getIdpAppConfiguration().isIgnoreValidation());
                    }

                    String metadataFile = getIdpAppConfiguration().getSpMetadataRootDir() + idp.getSpMetaDataFN();
                    log.debug("metadataFile:{}", metadataFile);

                    List<String> entityIdList = samlMetadataParser.getEntityIdFromMetadataFile(metadataFile);
                    log.debug("entityIdList:{}", entityIdList);
                    Set<String> duplicatesSet = new TreeSet<String>();
                    Set<String> entityIdSet = new TreeSet<String>();

                    if (entityIdList != null && !entityIdList.isEmpty()) {
                        for (String entityId : entityIdList) {
                            if (!entityIdSet.add(entityId)) {
                                duplicatesSet.add(entityId);
                            }
                        }
                    }

                    idp.setStatus(GluuStatus.ACTIVE);
                    validationLog = idp.getValidationLog();
                    log.debug("duplicatesSet:{}", duplicatesSet);
                    if (!duplicatesSet.isEmpty()) {
                        validationLog = this.getValidationLog(validationLog);
                        validationLog.add("This metadata contains multiple instances of entityId: "
                                + Arrays.toString(duplicatesSet.toArray()));
                    }
                    log.debug("errorHandler.isInternalError():{}", errorHandler.isInternalError());
                    if (errorHandler.isInternalError()) {
                        validationLog = idp.getValidationLog();
                        validationLog = this.getValidationLog(validationLog);
                        validationLog.add(
                                "Warning: cannot validate metadata. Check internet connetion ans www.w3.org availability.");

                        log.debug("errorHandler.getLog():{}", errorHandler.getLog());
                        // update log with warning
                        for (String warningLogMessage : errorHandler.getLog()) {
                            validationLog.add("Warning: " + warningLogMessage);
                        }
                    }
                    log.debug("Updating IdentityProvider:{} , validationLog :{}", idp, validationLog);
                
                    idpService.updateIdentityProvider(idp);
                    result = true;
                } else {
                    log.debug("Unhandled  metadataFN:{}", metadataFN);
                    idp.setValidationLog(new ArrayList<String>(new HashSet<String>(errorHandler.getLog())));
                    idp.setValidationStatus(ValidationStatus.FAILED);
                    idp.setStatus(GluuStatus.INACTIVE);
                    idpService.updateIdentityProvider(idp);
                }
            }
        }

        return result;
    }

    private List<String> getValidationLog(List<String> validationLog) {
        log.debug("validationLog:{}", validationLog);
        if (validationLog == null) {
            validationLog = new LinkedList<String>();
        }
        return validationLog;
    }
    
   private IdpAppConfiguration getIdpAppConfiguration() {
      return this.idpConfigService.getIdpAppConfiguration();
   }
}
