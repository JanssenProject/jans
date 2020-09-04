package org.gluu.configapi.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import javax.inject.Singleton;


@Singleton
public class ObjectMapperContextResolver implements ObjectMapperCustomizer {
	
	public void customize(ObjectMapper mapper) {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

}
