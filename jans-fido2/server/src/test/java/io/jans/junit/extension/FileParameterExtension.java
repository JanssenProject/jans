/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.junit.extension;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.util.StringHelper;

/**
 * @author Yuriy Movchan
 * @version 0.1, 17/02/2023
 */
public class FileParameterExtension implements ParameterResolver {

	Logger logger = LoggerFactory.getLogger(FileParameterExtension.class);

	private Map<String, String> parameters;

	public FileParameterExtension() throws IOException {
		logger.info("Loading test properties...");

        String propertiesFile = "target/test-classes/test.properties";

        // Load test parameters
        FileInputStream conf = new FileInputStream(propertiesFile);
        Properties prop;
        try {
            prop = new Properties();
            prop.load(conf);
        } finally {
            IOUtils.closeQuietly(conf);
        }

        parameters = new HashMap<String, String>();
        for (Entry<Object, Object> entry : prop.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (StringHelper.isEmptyString(key)) {
                continue;
            }
            parameters.put(key.toString(), value.toString());
        }
	}

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    	Name name = parameterContext.getParameter().getAnnotation(Name.class);
    	if (name == null) {
    		return null;
    	}

    	Class<?> parameterType = parameterContext.getParameter().getType();
    	if (parameterType.equals(String.class)) {
        	return parameters.get(name.value());
    	}

    	if (parameterType.equals(Integer.class)) {
        	return StringHelper.toInteger(parameters.get(name.value()));
    	}

    	return parameters.get(name.value());

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    	Name name = parameterContext.getParameter().getAnnotation(Name.class);
    	if (name == null) {
    		return false;
    	}
    	
    	boolean supports = parameters.containsKey(name.value());

    	if (!supports) {
    		logger.error("Parameter '{}' is not defined!", name.value());
    	}

    	Class<?> parameterType = parameterContext.getParameter().getType();
    	if (parameterType.equals(String.class) || parameterType.equals(Integer.class)) {
        	return supports;
    	}
    	
		logger.error("Parameter '{}' type isn't supported!", name.value());

		return false;
    }
    
}
