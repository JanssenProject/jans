package org.gluu.configapi.util;

import javax.ws.rs.ext.ParamConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class GenericEnumConverter<T extends Enum<T>> implements ParamConverter<T> {

	private static final Logger log = LoggerFactory.getLogger(GenericEnumConverter.class);
	private final BiMap<T, String> biMap = HashBiMap.create();

	public T fromString(String value) {
		T returnedValue = biMap.inverse().get(value);
		log.debug("Converting String \"{}\" to {}.{}", value, returnedValue.getClass(), returnedValue);
		return returnedValue;
	}

	public String toString(T t) {
		String returnedValue = biMap.get(t);
		log.debug("Converting Enum {}.{} to String \"{}\"", t.getClass(), t, returnedValue);
		return returnedValue;
	}

	public static <T extends Enum<T>> ParamConverter<T> of(Class<T> t) {
		//return new GenericEnumConverter<T>(t);
		return null;
	}

}
