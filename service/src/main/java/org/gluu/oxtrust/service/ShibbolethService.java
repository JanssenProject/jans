package org.gluu.oxtrust.service;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.gluu.oxtrust.model.GluuSAMLTrustRelationship;
import org.gluu.oxtrust.service.Shibboleth3ConfService;
import org.gluu.oxtrust.service.TrustService;
import org.gluu.oxtrust.util.CASProtocolConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ShibbolethService {

	private static final String IDP_SESSION_STORAGESERVICE = "idp.session.StorageService";
	private static final String IDP_CAS_STORAGESERVICE = "idp.cas.StorageService";
	private static final String CLIENT_SESSION_STORAGESERVICE = "shibboleth.ClientSessionStorageService";

	@Inject
	private Shibboleth3ConfService shibboleth3ConfService;

	@Inject
	private TrustService trustService;

	@Inject
	private SamlAcrService samlAcrService;
		
	public void update(CASProtocolConfiguration casProtocolConfiguration) {
		try {
			if (casProtocolConfiguration.isShibbolethEnabled()) {
				enable(casProtocolConfiguration);
			} else {
				disable(casProtocolConfiguration);
			}
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private void enable(CASProtocolConfiguration casProtocolConfiguration) throws ConfigurationException {
		PropertiesConfigurationLayout layoutConfiguration = idpPropertiesLayout();

		// CAS require server-side storage
		layoutConfiguration.getConfiguration().setProperty(IDP_SESSION_STORAGESERVICE,
				casProtocolConfiguration.getConfiguration().getSessionStorageType());
		layoutConfiguration.getConfiguration().setProperty(IDP_CAS_STORAGESERVICE,
				casProtocolConfiguration.getConfiguration().getSessionStorageType());
		layoutConfiguration.getConfiguration().save();

		// enable CAS beans in relying-party.xml
		updateShibboleth3Configuration();
	}

	private void disable(CASProtocolConfiguration casProtocolConfiguration) throws ConfigurationException {
		PropertiesConfigurationLayout layoutConfiguration = idpPropertiesLayout();

		// Restore default - client session storage
		layoutConfiguration.getConfiguration().setProperty(IDP_SESSION_STORAGESERVICE, CLIENT_SESSION_STORAGESERVICE);
		layoutConfiguration.getConfiguration().setProperty(IDP_CAS_STORAGESERVICE,
				casProtocolConfiguration.getConfiguration().getSessionStorageType());
		layoutConfiguration.getConfiguration().save();

		// disable CAS beans in relying-party.xml
		updateShibboleth3Configuration();

	}

	private PropertiesConfigurationLayout idpPropertiesLayout() throws ConfigurationException {
		String idpConfFolder = shibboleth3ConfService.getIdpConfDir();
		PropertiesConfiguration idpPropertiesConfiguration = new PropertiesConfiguration(
				idpConfFolder + Shibboleth3ConfService.SHIB3_IDP_PROPERTIES_FILE);
		return new PropertiesConfigurationLayout(idpPropertiesConfiguration);
	}

	private void updateShibboleth3Configuration() {
		List<GluuSAMLTrustRelationship> trustRelationships = trustService.getAllActiveTrustRelationships();
		shibboleth3ConfService.generateConfigurationFiles(trustRelationships);
		shibboleth3ConfService.generateConfigurationFiles(samlAcrService.getAll());
	}

}
