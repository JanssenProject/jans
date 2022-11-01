package io.jans.ca.rs.protect;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/05/2016
 */

public class StaticStorage {

    private static Map<Class<?>, Object> dataMap = Maps.newConcurrentMap();

    public static <T> T get(Class<T> type) {
        return (T) dataMap.get(type);
    }

    public static <T> void put(Class<T> type, T data) {
        dataMap.put(type, data);
    }
}
