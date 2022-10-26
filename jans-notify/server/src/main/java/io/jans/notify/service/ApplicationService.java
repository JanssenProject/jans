/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.jans.notify.model.conf.AccessConfiguration;
import io.jans.notify.model.conf.ClientConfiguration;
import io.jans.notify.model.conf.Configuration;
import io.jans.notify.model.conf.PlatformConfiguration;
import io.jans.notify.model.sns.ClientData;
import org.slf4j.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@ApplicationScoped
@Named
public class ApplicationService {

	@Inject
	private Logger log;

	@Inject
	private Configuration configuration;

	@Inject
	private AccessConfiguration accessConfiguration;

	private Map<String, ClientData> platormClients;

	private Map<String, ClientConfiguration> accessClients;

	@PostConstruct
	public void create() {
		this.platormClients = new HashMap<String, ClientData>();
		this.accessClients = new HashMap<String, ClientConfiguration>();
	}

	public void init() {
		initPlatformClients();
		initAccessClients();
	}

	public ClientConfiguration getAccessClient(String accessKeyId, String secretAccessKey) {
		if ((accessKeyId == null) || (secretAccessKey == null)) {
			log.error("Access key or secret is empty");
			return null;
		}

		ClientConfiguration clientConfiguration = this.accessClients.get(accessKeyId);
		if (clientConfiguration == null) {
			log.error("Failed to find client '{}' configuration", accessKeyId);
			return null;
		}

		if (!secretAccessKey.equals(clientConfiguration.getSecretAccessKey())) {
			log.error("Secret access key is invalid for client '{}'", accessKeyId);
			return null;
		}

		return clientConfiguration;
	}

	public ClientData getClientDataByPlatformId(String platformId) {
		if (platformId == null) {
			log.error("Request platform is empty");
			return null;
		}

		ClientData clientData = this.platormClients.get(platformId.toLowerCase());
		if (clientData == null) {
			log.error("Failed to find client data for platform '{}'", platformId);
			return null;
		}

		return clientData;
	}

	private void initPlatformClients() {
		List<PlatformConfiguration> platformConfigurations = configuration.getPlatformConfigurations();
		if ((platformConfigurations == null) || platformConfigurations.isEmpty()) {
			log.error("List of platforms is empty!");
			return;
		}

		for (PlatformConfiguration platformConfiguration : platformConfigurations) {
			if (platformConfiguration.isEnabled()) {
				ClientData clientData = createClientData(platformConfiguration);
				if (clientData != null) {
					this.platormClients.put(platformConfiguration.getPlatformId().toLowerCase(), clientData);
				}
			}
		}

		log.info("Loaded configurations for '{}' clients", this.platormClients.size());
	}

	private void initAccessClients() {
		List<ClientConfiguration> clientConfiguratios = accessConfiguration.getClientConfigurations();
		if ((clientConfiguratios == null) || clientConfiguratios.isEmpty()) {
			log.error("List of clients is empty!");
			return;
		}

		for (ClientConfiguration clientConfiguration : clientConfiguratios) {
			if (clientConfiguration.isEnabled()) {
				this.accessClients.put(clientConfiguration.getAccessKeyId(), clientConfiguration);
			}
		}

		log.info("Loaded configurations for '{}' access clients", this.accessClients.size());
	}

	private ClientData createClientData(PlatformConfiguration platformConfiguration) {
		ClientData clientData = null;
		try {
			BasicAWSCredentials credentials = new BasicAWSCredentials(platformConfiguration.getAccessKeyId(),
					platformConfiguration.getSecretAccessKey());
			AmazonSNSClientBuilder snsClientBuilder = AmazonSNSClientBuilder.standard();
			AmazonSNS amazonSNS = snsClientBuilder.withCredentials(new AWSStaticCredentialsProvider(credentials))
					.withRegion(platformConfiguration.getRegion()).build();
			clientData = new ClientData(amazonSNS, platformConfiguration.getPlatform(),
					platformConfiguration.getPlatformArn());
		} catch (Exception ex) {
			log.error("Faield to create client", ex);
		}

		return clientData;
	}

    public String asJsonSilently(Object obj) {
        try {
            return asJson(obj);
        } catch (IOException ex) {
            log.trace("Failed to convert object to JSON", ex);
            return "";
        }
    }

    public String asPrettyJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p_object);
    }

    public String asJson(Object p_object) throws IOException {
        final ObjectMapper mapper = createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        return mapper.writeValueAsString(p_object);
    }

    public <T> T jsonToObject(String json, Class<T> clazz) throws JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
        T clazzObject = mapper.readValue(json, clazz);

		return clazzObject;
	}

    public ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = AnnotationIntrospector.pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);
        return mapper;
    }

}
