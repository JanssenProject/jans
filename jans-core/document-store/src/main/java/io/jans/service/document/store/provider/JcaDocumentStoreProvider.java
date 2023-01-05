/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.JcaDocumentStoreConfiguration;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class JcaDocumentStoreProvider extends DocumentStoreProvider<JcaDocumentStoreProvider> {

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
		} catch (RepositoryException ex) {
			log.error("Failed to check if path '" + path + "' exists in repository", ex);
		}

		return fileNode != null;
	}

	@Override
	public boolean saveDocument(String path, String documentContent, Charset charset, List<String> moduleList) {
		log.debug("Save document: '{}'", path);
		
		String normalizedPath = getNormalizedPath(path);
		try {
			Session session = getSessionWithTimeout();
			try {
				Node contentNode = getOrCreateContentNode(normalizedPath, session);
				Value value = session.getValueFactory().createValue(documentContent);
				contentNode.setProperty("jcr:data", value);

				session.save();
				return true;
			} finally {
				closeSession(session);
			}
		} catch (RepositoryException ex) {
			log.error("Failed to write document to file '{}'", path, ex);
		}

		return false;
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream, List<String> moduleList) {
		log.debug("Save document from stream: '{}'", path);

		String normalizedPath = getNormalizedPath(path);
		try {
			Session session = getSessionWithTimeout();
			try {
				Node contentNode = getOrCreateContentNode(normalizedPath, session);
				Binary value = session.getValueFactory().createBinary(documentStream);
				contentNode.setProperty("jcr:data", value);

				session.save();
				return true;
			} finally {
				closeSession(session);
			}
		} catch (RepositoryException ex) {
			log.error("Failed to write document from stream to file '{}'", path, ex);
		}

		return false;
	}


	@Override
	public String readDocument(String path, Charset charset) {
		log.debug("Read document: '{}'", path);

		String normalizedPath = getNormalizedPath(path);
		try {
			Session session = getSessionWithTimeout();
			try {
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
			} finally {
				closeSession(session);
			}
		} catch (IOException ex) {
		} catch (RepositoryException ex) {
			log.error("Failed to read document from file '{}'", path, ex);
		}
		
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String path) {
		log.debug("Read document as stream: '{}'", path);

		String normalizedPath = getNormalizedPath(path);
		try {
			Session session = getSessionWithTimeout();
			try {
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
			} finally {
				closeSession(session);
			}
		} catch (IOException ex) {
		} catch (RepositoryException ex) {
			log.error("Failed to read document as stream from file '{}'", path, ex);
		}
		
		return null;
	}

	@Override
	public boolean renameDocument(String currentPath, String destinationPath) {
		log.debug("Rename document: '{}' -> '{}'", currentPath, destinationPath);

		String normalizedCurrentPath = getNormalizedPath(currentPath);
		String normalizedDestinationPath = getNormalizedPath(destinationPath);

		try {
			Session session = getSessionWithTimeout();
			try {
				removeDocument(normalizedDestinationPath, session);

				createPath(normalizedDestinationPath, session);
				session.move(normalizedCurrentPath, normalizedDestinationPath);

				session.save();
				return true;
			} finally {
				closeSession(session);
			}
		} catch (RepositoryException ex) {
			log.error("Failed to rename to destination file '{}'", destinationPath, ex);
		}
		
		return false;
	}

	@Override
	public boolean removeDocument(String path) {
		log.debug("Remove document: '{}'", path);

		try {
			Session session = getSessionWithTimeout();
			try {
				removeDocument(path, session);

				session.save();
				return true;
			} finally {
				closeSession(session);
			}
		} catch (RepositoryException ex) {
			log.error("Failed to remove document file '{}'", path, ex);
		}

		return false;
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

}
