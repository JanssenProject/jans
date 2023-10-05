package io.jans.casa.plugins.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.NotifyChange;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that holds the logic to list and enroll sample creds
 * 
 * @author madhumita
 *
 */

public class SampleService {

	private static SampleService SINGLE_INSTANCE = null;
	public static Map<String, String> properties;
	private Logger logger = LoggerFactory.getLogger(getClass());
	public static String ACR = "basic";

	private IPersistenceService persistenceService;

	private SampleService() {
		persistenceService = Utils.managedBean(IPersistenceService.class);
		reloadConfiguration();

	}

	public static SampleService getInstance() {
		if (SINGLE_INSTANCE == null) {
			synchronized (SampleService.class) {
				SINGLE_INSTANCE = new SampleService();
			}
		}
		return SINGLE_INSTANCE;
	}

	public void reloadConfiguration() {
		ObjectMapper mapper = new ObjectMapper();
		properties = persistenceService.getCustScriptConfigProperties(ACR);
		if (properties == null) {
			logger.warn(
					"Config. properties for custom script '{}' could not be read. Features related to {} will not be accessible",
					ACR, ACR.toUpperCase());
		} else {
			try {
				logger.info("Sample settings found were: {}", mapper.writeValueAsString(properties));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public String getScriptPropertyValue(String value) {
		return properties.get(value);
	}

	public List<BasicCredential> getDevices(String uniqueIdOfTheUser) {
		// Write the code to connect to the 3rd party API and fetch credentials against
		// the user
		List<BasicCredential> list = new ArrayList<BasicCredential>();
		list.add(new BasicCredential("test device 1", System.currentTimeMillis()));
		list.add(new BasicCredential("test device 2", System.currentTimeMillis()));
		return list;
	}

	public int getDeviceTotal(String uniqueIdOfTheUser) {
		// Write the code to connect to the 3rd party API and fetch total number of
		// credentials against the user
		return 0;
	}

	public boolean deleteSampleDevice(String userName, String deviceId) {
		// write the logic for deleting the device
		return true;
	}

	public boolean updateSampleDevice(String userName, String oldName, String newName) {
		// write the logic for updating the device
		return true;
	}

}
