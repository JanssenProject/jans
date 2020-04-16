package org.gluu.service.document.store.conf;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class DocumentStoreConfiguration implements Serializable {

	private static final long serialVersionUID = 2519892725606554887L;

	private DocumentStoreType documentStoreType;

    private LocalDocumentStoreConfiguration localConfiguration;

    private JcaDocumentStoreConfiguration jcaConfiguration;

    private WebDavDocumentStoreConfiguration webDavConfiguration;

    public DocumentStoreType getDocumentStoreType() {
		return documentStoreType;
	}

	public void setDocumentStoreType(DocumentStoreType documentStoreType) {
		this.documentStoreType = documentStoreType;
	}

	public LocalDocumentStoreConfiguration getLocalConfiguration() {
		return localConfiguration;
	}

	public void setLocalConfiguration(LocalDocumentStoreConfiguration localConfiguration) {
		this.localConfiguration = localConfiguration;
	}

	public JcaDocumentStoreConfiguration getJcaConfiguration() {
		return jcaConfiguration;
	}

	public void setJcaConfiguration(JcaDocumentStoreConfiguration jcaConfiguration) {
		this.jcaConfiguration = jcaConfiguration;
	}

	public WebDavDocumentStoreConfiguration getWebDavConfiguration() {
		return webDavConfiguration;
	}

	public void setWebDavConfiguration(WebDavDocumentStoreConfiguration webDavConfiguration) {
		this.webDavConfiguration = webDavConfiguration;
	}

	@Override
	public String toString() {
		return "DocumentStoreConfiguration [documentStoreType=" + documentStoreType + ", localConfiguration=" + localConfiguration
				+ ", jcaConfiguration=" + jcaConfiguration + ", webDavConfiguration=" + webDavConfiguration + "]";
	}
}
