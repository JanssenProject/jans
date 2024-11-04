/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.manager;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.model.Document;
import io.jans.service.document.store.service.DocumentStoreService;
import io.jans.service.document.store.service.cdi.event.ReloadDocument;
import io.jans.service.document.store.service.cdi.event.UpdateDocumentStoreEvent;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

/**
 * Provides actual versions of scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@ApplicationScoped
public class DocumentStoreManager {

	public static final String DOCUMENT_DATA_MODIFIED_EVENT_TYPE = "documentDataModifiedEvent";
	public static final int DEFAULT_INTERVAL = 30; // 30 seconds

	protected static final String[] CUSTOM_SCRIPT_CHECK_ATTRIBUTES = { "dn", "inum", "creationDate", "jansRevision", "jansEnabled", "jansFilePath", "displayName" };
	@Inject
	protected Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	@ReloadDocument
	private Event<String> event;

	@Inject
	private DocumentStoreService documentStoreService;

	private AtomicBoolean isActive;
	private long lastFinishedTime;
	
    private boolean initialized = false;
	private List<String> supportedServiceTypes = new ArrayList();
	private DocumentStoreType previousServiceType;
	
	private Map<String, Object> loadedDocumentsStore = new TreeMap<String, Object>();

	@Asynchronous
	public void initTimer(List<String> serviceTypes) {
		initTimer(serviceTypes, DEFAULT_INTERVAL);
	}

	@Asynchronous
	public void initTimer(List<String> serviceTypes, int interval) {
		this.supportedServiceTypes.addAll(serviceTypes);

		configure();

		final int delay = 30;

		reload(true);

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, DEFAULT_INTERVAL), new UpdateDocumentStoreEvent(),
				Scheduled.Literal.INSTANCE));
		
		initialized = true;
	}

	protected void configure() {
		this.isActive = new AtomicBoolean(false);
		this.lastFinishedTime = System.currentTimeMillis();
	}

	public void reloadTimerEvent(@Observes @Scheduled UpdateDocumentStoreEvent updateDocumentStoreEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reload(false);
		} catch (Throwable ex) {
			log.error("Exception happened while reloading custom scripts configuration", ex);
		} finally {
			this.isActive.set(false);
			this.lastFinishedTime = System.currentTimeMillis();
			log.trace("Last finished time '{}'", new Date(this.lastFinishedTime));
		}
	}

	public void destroy(@BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
		log.debug("Destroying documents store");
		
		loadedDocumentsStore.clear();
	}

	private void reload(boolean syncUpdate) {
		boolean modified = reloadImpl();

		if (modified || syncUpdate) {
			event.fire(DOCUMENT_DATA_MODIFIED_EVENT_TYPE);
		}
	}

	private boolean reloadImpl() {
		// Load current documents revisions
		DocumentStoreType currentServiceType = documentStoreService.getProviderType();
		boolean storeTypeChanged = currentServiceType != previousServiceType;
		boolean supportedServiceType = DocumentStoreType.DB == currentServiceType;

		if (storeTypeChanged) {
			log.warn("Document store type were changed!!!");
		}

		previousServiceType = currentServiceType;
		List<Object> loadedDocuments;
		// Service supports only DB Document Store
		if (!supportedServiceType || supportedServiceTypes.isEmpty()) {
			loadedDocuments = new ArrayList<>();
		} else {
			loadedDocuments = documentStoreService.findDocumentsByModules(supportedServiceTypes, CUSTOM_SCRIPT_CHECK_ATTRIBUTES);
		}

		// Store updated documents
		ReloadResult reloadResult = updateDocuments(this.loadedDocumentsStore, loadedDocuments);
		this.loadedDocumentsStore = reloadResult.getDocumentsStore();

		return reloadResult.isModified();
	}

	private class ReloadResult {
		private Map<String, Object> documentsStore;
		private boolean modified;

		ReloadResult(Map<String, Object> documentsStore, boolean modified) {
			this.documentsStore = documentsStore;
			this.modified = modified;
		}

		public Map<String, Object> getDocumentsStore() {
			return documentsStore;
		}

		public boolean isModified() {
			return modified;
		}
	}

	private ReloadResult updateDocuments(Map<String, Object> prevDocumentsStore, List<Object> newLoadedDocuments) {
		boolean modified = false;
		Map<String, Object> newDocumentStore;
		if (prevDocumentsStore == null) {
			newDocumentStore = new HashMap<>();
			modified = true;
		} else {
			// Clone old map to avoid rewrite not changed files
			newDocumentStore = new HashMap<>(prevDocumentsStore);
		}

		List<String> newLoadedDocumentsInums = new ArrayList<>();
		for (Object newDocumentObject : newLoadedDocuments) {
			Document newDocument = (Document) newDocumentObject;
			if (!newDocument.isEnabled()) {
				continue;
			}

			String newDocumentInum = StringHelper.toLowerCase(newDocument.getInum());
			newLoadedDocumentsInums.add(newDocumentInum);

			Document prevDocument = (Document) newDocumentStore.get(newDocumentInum);
			if ((prevDocument == null) || prevDocument.getRevision() != newDocument.getRevision()) {
				// Load document entry with all attributes
				
				// Store file in file system
				saveDocument(newDocument);

				// Store configuration and script
				newDocumentStore.put(newDocumentInum, newDocument);

				modified = true;
			}
		}

		// Remove old external scripts configurations
		for (Iterator<Entry<String, Object>> it = newDocumentStore.entrySet().iterator(); it.hasNext();) {
			Entry<String, Object> newDocumentStoreEntry = it.next();

			String prevDocumentStoreEntryInum = newDocumentStoreEntry.getKey();
			if (!newLoadedDocumentsInums.contains(prevDocumentStoreEntryInum)) {
				// Remove old document
				removeDocument((Document) newDocumentStoreEntry.getValue());
				it.remove();

				modified = true;
			}
		}

		return new ReloadResult(newDocumentStore, modified);
	}

	private void saveDocument(Document document) {
		try (InputStream is = documentStoreService.readBinaryDocumentAsStream(document.getFileName())) {
			log.info("Writing file {} as stream", document.getFileName());
			
			Path path = Paths.get(document.getFilePath(), document.getFileName());
			File file = path.toFile();
			try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
				IOUtils.copy(is, os);
			}
			log.info("File {} was created successfully", document.getFileName());
		} catch (Exception ex) {
			log.error("Failed to write file {} as stream", document.getFileName(), ex);
		}
	}

	private void removeDocument(Document document) {
		try {
			File file = new File(document.getFileName());
			
			boolean res = file.delete();
			if (res) {
				log.info("File was {} removed successfully", document.getFileName());
			} else {
				log.error("Failed to remove file {}", document.getFileName());
			}
		} catch (Exception ex) {
			log.error("Failed to remove file {}", document.getFileName(), ex);
		}
	}

	public List<String> getSupportedServiceTypes() {
		return supportedServiceTypes;
	}

	public boolean isSupportedServiceType(List<String> serviceTypes) {
		return supportedServiceTypes.containsAll(serviceTypes);
	}

	public boolean isInitialized() {
		return initialized;
	}

}
