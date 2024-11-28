/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.conf.JcaDocumentStoreConfiguration;
import io.jans.service.document.store.exception.DocumentException;
import io.jans.service.document.store.exception.WriteDocumentException;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class JcaDocumentStoreProvider extends DocumentStoreProvider<String> {

	@Inject
	private Logger log;
	

	@Inject
	private DocumentStoreConfiguration documentStoreConfiguration;

    @Inject
    private StringEncrypter stringEncrypter;

	private JcaDocumentStoreConfiguration jcaDocumentStoreConfiguration;

	private URLRemoteRepository repository;
	private SimpleCredentials credentials;
	private String workspaceName;
	private long connectionTimeout; 

	public JcaDocumentStoreProvider() {
	}

	@PostConstruct
	public void init() {
		this.jcaDocumentStoreConfiguration = documentStoreConfiguration.getJcaConfiguration();
	}

	public void create() {
		try {
			log.debug("Starting JcaDocumentStoreProvider ...");
			decryptPassword(jcaDocumentStoreConfiguration);
			this.repository = new URLRemoteRepository(jcaDocumentStoreConfiguration.getServerUrl());
			
			String password = StringUtils.isBlank(jcaDocumentStoreConfiguration.getDecryptedPassword()) ? "" : jcaDocumentStoreConfiguration.getDecryptedPassword();
			
			this.credentials = new SimpleCredentials(jcaDocumentStoreConfiguration.getUserId(),
					password.toCharArray());
			
			this.workspaceName = jcaDocumentStoreConfiguration.getWorkspaceName();
			this.connectionTimeout = jcaDocumentStoreConfiguration.getConnectionTimeout();
	    } catch (Exception ex) {
	        throw new IllegalStateException("Error starting JcaDocumentStoreProvider", ex);
	    }
	}

	public void configure(DocumentStoreConfiguration documentStoreConfiguration, StringEncrypter stringEncrypter) {
		this.log = LoggerFactory.getLogger(DocumentStoreConfiguration.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
		this.stringEncrypter = stringEncrypter;
	}

	@PreDestroy
	public void destroy() {
		log.debug("Destroying JcaDocumentStoreProvider");

		this.repository = null;

		log.debug("Destroyed JcaDocumentStoreProvider");
	}

	public void setJcaDocumentStoreConfiguration(JcaDocumentStoreConfiguration jcaDocumentStoreConfiguration) {
		this.jcaDocumentStoreConfiguration = jcaDocumentStoreConfiguration;
	}

	public DocumentStoreType getProviderType() {
		return DocumentStoreType.JCA;
	}

	@Override
	public boolean hasDocument(String path) {
		log.debug("Has document: '{}'", path);

		if (StringHelper.isEmpty(path)) {
			throw new IllegalArgumentException("Specified path should not be empty!");
		}

		Node fileNode = null;
		Session session;
		try {
			session = getSessionWithTimeout();
			try {
				fileNode = JcrUtils.getNodeIfExists(getNormalizedPath(path), session);
			} finally {
				closeSession(session);
			}
		} catch (Exception ex) {
			log.error("Failed to check if path '" + path + "' exists in repository", ex);
			throw new DocumentException(ex);
		}

		return fileNode != null;
	}

	@Override
	public String saveDocument(String path, String description, String documentContent, Charset charset, String module) {
		log.debug("Save document: '{}'", path);
		
		String normalizedPath = getNormalizedPath(path);
		Session session = null;
		try {
			session = getSessionWithTimeout();
			Node contentNode = getOrCreateContentNode(normalizedPath, session);
			Value value = session.getValueFactory().createValue(documentContent);
			contentNode.setProperty("jcr:data", value);
			contentNode.setProperty(JcrConstants.JCR_DATA, description);

			session.save();
			return path;
		} catch (Exception ex) {
			log.error("Failed to write document to file '{}'", path, ex);
			throw new WriteDocumentException(ex);
		} finally {
			closeSession(session);
		}
	}

	@Override
	public String saveDocumentStream(String path, String description, InputStream documentStream, String module) {
		log.debug("Save document from stream: '{}'", path);

		String normalizedPath = getNormalizedPath(path);
		Session session = null;
		try {
			session = getSessionWithTimeout();
			Node contentNode = getOrCreateContentNode(normalizedPath, session);
			Binary value = session.getValueFactory().createBinary(documentStream);
			contentNode.setProperty("jcr:data", value);
			contentNode.setProperty(JcrConstants.JCR_DATA, description);

			session.save();
			return path;
		} catch (Exception ex) {
			log.error("Failed to write document from stream to file '{}'", path, ex);
			throw new WriteDocumentException(ex);
		} finally {
			closeSession(session);
		}
	}

	@Override
	public String saveBinaryDocumentStream(String path, String description, InputStream documentStream,
			String module) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public String readDocument(String path, Charset charset) {
		log.debug("Read document: '{}'", path);

		String normalizedPath = getNormalizedPath(path);
		Session session = null;
		try {
			session = getSessionWithTimeout();
			Node fileNode = JcrUtils.getNodeIfExists(normalizedPath, session);
			if (fileNode == null) {
				log.error("Document file '{}' isn't exist", path);
				return null;
			}

			Node contentNode = fileNode.getNode(JcrConstants.JCR_CONTENT);
			Property property = contentNode.getProperty("jcr:data");
			try (InputStream in = property.getBinary().getStream()) {
				return IOUtils.toString(in, charset);
			}

		} catch (Exception ex) {
			log.error("Failed to read document from file '{}'", path, ex);
			throw new DocumentException();
		} finally {
			closeSession(session);
		}
	}

	@Override
	public InputStream readDocumentAsStream(String path) {
		log.debug("Read document as stream: '{}'", path);

		String normalizedPath = getNormalizedPath(path);
		Session session = null;
		try {
			session = getSessionWithTimeout();
			Node fileNode = JcrUtils.getNodeIfExists(normalizedPath, session);
			if (fileNode == null) {
				log.error("Document file '{}' isn't exist", path);
				return null;
			}

			Node contentNode = fileNode.getNode(JcrConstants.JCR_CONTENT);
			Property property = contentNode.getProperty("jcr:data");
			try (InputStream in = property.getBinary().getStream()) {
				// Note: We can't return real input stream because we need to make sure that we close session
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				IOUtils.copy(in, bos);

				return new ByteArrayInputStream(bos.toByteArray());
			}
		} catch (Exception ex) {
			log.error("Failed to read document as stream from file '{}'", path, ex);
			throw new DocumentException();
		} finally {
			closeSession(session);
		}
	}

	@Override
	public InputStream readBinaryDocumentAsStream(String path) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public String renameDocument(String currentPath, String destinationPath) {
		log.debug("Rename document: '{}' -> '{}'", currentPath, destinationPath);

		String normalizedCurrentPath = getNormalizedPath(currentPath);
		String normalizedDestinationPath = getNormalizedPath(destinationPath);
		Session session = null;
		try {
			session = getSessionWithTimeout();
			removeDocument(normalizedDestinationPath, session);

			createPath(normalizedDestinationPath, session);
			session.move(normalizedCurrentPath, normalizedDestinationPath);

			session.save();
			return destinationPath;

		} catch (Exception ex) {
			log.error("Failed to rename to destination file '{}'", destinationPath, ex);
			throw new DocumentException();
		} finally {
			closeSession(session);
		}
	}

	@Override
	public boolean removeDocument(String path) {
		log.debug("Remove document: '{}'", path);
		Session session = null;
		try {
			session = getSessionWithTimeout();
			removeDocument(path, session);

			session.save();
			return true;

		} catch (Exception ex) {
			log.error("Failed to remove document file '{}'", path, ex);
			throw new DocumentException();
		} finally {
			closeSession(session);
		}
	}

	private void removeDocument(String path, Session session)
			throws RepositoryException, VersionException, LockException, ConstraintViolationException, AccessDeniedException {
		Node fileNode = JcrUtils.getNodeIfExists(getNormalizedPath(path), session);
		if (fileNode != null) {
			fileNode.remove();
		}
	}

    private void decryptPassword(JcaDocumentStoreConfiguration jcaDocumentStoreConfiguration) {
        try {
            String encryptedPassword = jcaDocumentStoreConfiguration.getPassword();
            if (StringUtils.isNotBlank(encryptedPassword)) {
            	jcaDocumentStoreConfiguration.setDecryptedPassword(stringEncrypter.decrypt(encryptedPassword));
                log.trace("Decrypted JCA password successfully.");
            }
        } catch (StringEncrypter.EncryptionException ex) {
            log.error("Error during JCA password decryption", ex);
        }
    }

	private Session getSession() throws RepositoryException {
		return repository.login(credentials, workspaceName);
	}

	public Session getSessionWithTimeout() throws RepositoryException {
		try {
			CallableSession cs = new CallableSession(repository, credentials, workspaceName);
			FutureTask<Session> future = new FutureTask<Session>(cs);
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(future);
			return future.get(connectionTimeout * 1000, TimeUnit.MILLISECONDS);
		} catch (Exception ex) {
			throw new RepositoryException("Failed to get session", ex);
		}
	}

	private void closeSession(Session session) {
		if (session != null && session.isLive()) {
			session.logout();
		}
	}

	private String getNormalizedPath(String path) {
		return path.replaceAll("\\\\",  "/");
	}

	private Node getOrCreateContentNode(String path, Session session) throws RepositoryException {
		Node fileNode = JcrUtils.getNodeIfExists(getNormalizedPath(path), session);
		Node contentNode = null;
		if (fileNode == null) {
			fileNode = JcrUtils.getOrCreateByPath(path, NodeType.NT_FOLDER, NodeType.NT_FILE, session, false);
			contentNode = fileNode.addNode(JcrConstants.JCR_CONTENT, NodeType.NT_RESOURCE);
		} else {
			contentNode = fileNode.getNode(JcrConstants.JCR_CONTENT);
		}

		return contentNode;
	}

	private void createPath(String path, Session session) throws RepositoryException {
		File filePath = new File(path);
		String folderPath = filePath.getParentFile().getPath();

		String normalizedFolderPath = getNormalizedPath(folderPath);
		JcrUtils.getOrCreateByPath(normalizedFolderPath, NodeType.NT_FOLDER, session);
	}

	class CallableSession implements Callable<Session> {

		private URLRemoteRepository repository;
		private SimpleCredentials credentials;
		private String workspaceName;

		public CallableSession(URLRemoteRepository repository, SimpleCredentials credentials, String workspaceName) {
			this.repository = repository;
			this.credentials = credentials;
			this.workspaceName = workspaceName;
		}

		@Override
		public Session call() throws Exception {
			return repository.login(credentials, workspaceName);
		}
	}

	public boolean isConnected() {
		log.debug("Check if server available");

		Node fileNode = null;
		Session session;
		try {
			session = getSessionWithTimeout();
			try {
				fileNode = JcrUtils.getNodeIfExists(getNormalizedPath("/"), session);
			} finally {
				closeSession(session);
			}
		} catch (RepositoryException ex) {
			log.error("Failed to check if server available", ex);
		}

		return fileNode != null;
	}

	@Override
	public List<String> findDocumentsByModules(List<String> moduleList, String ... attributes) {
        throw new RuntimeException("Not yet implemented");
	}

}
