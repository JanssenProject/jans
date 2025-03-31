/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.external;

import java.util.Map;
//import javax.inject.Named;

import io.jans.link.model.GluuCustomPerson;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.bind.BindCredentials;
import io.jans.model.custom.script.type.user.LinkInterceptionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Provides factory methods needed to create external link interception extension
 * 
 * @author Yuriy Movchan Date: 01/12/2015
 */
@ApplicationScoped
@Named("externalLinkService")
public class ExternalLinkService extends ExternalScriptService {

	private static final long serialVersionUID = 1707751544454591273L;

	public ExternalLinkService() {
		super(CustomScriptType.LINK_INTERCEPTION);
	}

	public boolean executeExternalUpdateUserMethod(CustomScriptConfiguration customScriptConfiguration, GluuCustomPerson user) {
		try {
			log.debug("Executing python 'updateUser' method");
			LinkInterceptionType externalType = (LinkInterceptionType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalType.updateUser(user, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}

		return false;
	}

    public BindCredentials executeExternalGetBindCredentialsMethod(CustomScriptConfiguration customScriptConfiguration, String configId) {
        try {
            log.debug("Executing python 'getBindCredentialsMethod' method");
            LinkInterceptionType externalType = (LinkInterceptionType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            
            // Execute only if API > 1
            if (externalType.getApiVersion() > 1) {
                return externalType.getBindCredentials(configId, configurationAttributes);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return null;
    }

    public boolean executeExternalIsStartProcessMethod(CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.debug("Executing python 'executeExternalIsStartProcessMethod' method");
            LinkInterceptionType externalType = (LinkInterceptionType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            
            // Execute only if API > 2
            if (externalType.getApiVersion() > 2) {
                return externalType.isStartProcess(configurationAttributes);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

	public boolean executeExternalUpdateUserMethods(GluuCustomPerson user) {
		boolean result = true;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			result &= executeExternalUpdateUserMethod(customScriptConfiguration, user);
			if (!result) {
				return result;
			}
		}

		return result;
	}

    public BindCredentials executeExternalGetBindCredentialsMethods(String configId) {
        BindCredentials result = null;
        if(this.customScriptConfigurations == null){
            reload(CustomScriptType.LINK_INTERCEPTION.toString());
        }
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            result = executeExternalGetBindCredentialsMethod(customScriptConfiguration, configId);
            if (result != null) {
                return result;
            }
        }

        return result;
    }

	public boolean executeExternalIsStartProcessMethods() {
        if(this.customScriptConfigurations == null){
            reload(CustomScriptType.LINK_INTERCEPTION.toString());
        }
		boolean result = this.customScriptConfigurations.size() > 0;

		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			result &= executeExternalIsStartProcessMethod(customScriptConfiguration);
			if (!result) {
				return result;
			}
		}

		return result;
	}

}
