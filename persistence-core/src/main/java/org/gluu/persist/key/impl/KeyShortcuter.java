package org.gluu.persist.key.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.reflect.property.PropertyAnnotation;
import org.gluu.persist.reflect.util.ReflectHelper;
import org.gluu.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Utility class which provides shorter version of the key
 *
 * @author Yuriy Zabrovarnyy
 */
public class KeyShortcuter {

    private static final Logger LOG = LoggerFactory.getLogger(KeyShortcuter.class);

    public static final String CONF_FILE_NAME = "key-shortcuter-rules.json";

    private static final List<Class> PROCESSED_ENTRIES = Lists.newArrayList();

    private static final BiMap<String, String> MAP = HashBiMap.create();
    private static final BiMap<String, String> INVERSE_MAP = MAP.inverse();

    private KeyShortcuter() {
    }

    private static KeyShortcuterConf conf = load();

    private static KeyShortcuterConf load() {
        try (InputStream is = KeyShortcuter.class.getResourceAsStream("/" + CONF_FILE_NAME)) {
            return Util.createJsonMapper().readValue(is, KeyShortcuterConf.class);
        } catch (IOException e) {
            LOG.error("Failed to load key shortcuter configuration from file: " + CONF_FILE_NAME, e);
            return null;
        }
    }

    public static String fromShortcut(String shortcut) {
        return INVERSE_MAP.getOrDefault(shortcut, shortcut);
    }

    public static String shortcut(String key) {
        if (conf == null) {
            LOG.error("Failed to load key shortcuter configuration from file: " + CONF_FILE_NAME);
            return key;
        }
        if (StringUtils.isBlank(key)) {
            return key;
        }

        final String cachedShortcut = MAP.get(key);
        if (cachedShortcut != null) {
            return cachedShortcut;
        }

        String copy = key;

        for (String prefix : conf.getPrefixes()) {
            if (key.startsWith(prefix)) {
                key = StringUtils.removeStart(key, prefix);
            }
        }

        for (Map.Entry<String, String> replace : conf.getReplaces().entrySet()) {
            key = StringUtils.replace(key, replace.getKey(), replace.getValue());
        }

        key = lowercaseFirstChar(key);
        try {
            MAP.put(copy, key);
        } catch (IllegalArgumentException e) {
            LOG.error("Found duplicate for key: " + key + ", duplicate from: " + MAP.inverse().get(key));
            return copy; // skip shortcuting and return original key
        }
        return key;
    }

    public static String lowercaseFirstChar(String key) {
        return Character.toLowerCase(key.charAt(0)) + key.substring(1);
    }

    public static <T> void initIfNeeded(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations) {
        if (entryClass == null || propertiesAnnotations == null || PROCESSED_ENTRIES.contains(entryClass)) {
            return;
        }

        for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
            Annotation annotation = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), AttributeName.class);
            if (annotation instanceof AttributeName) {
                shortcut(((AttributeName) annotation).name());
            }
        }

        PROCESSED_ENTRIES.add(entryClass);
    }
}
