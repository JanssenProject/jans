package org.gluu.persist.key.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class which provides shorter version of the key
 *
 * @author Yuriy Zabrovarnyy
 */
public class KeyShortcuter {

    // Order important !
    private static final List<String> PREFIXES = Lists.newArrayList(
            "gluu", "oxAuth", "oxTrust", "ox"
    );

    // Order important !
    private static final Map<String, String> REPLACES = new HashMap<String, String>()
    {{
        // with markers
        put("Group", "_g");
        put("Client", "_c");
        put("Type", "_t");
        put("User", "_u");
        put("Default", "_d");

        // without markers
        put("Configuration", "Conf");
        put("Attribute", "Attr");
        put("Application", "App");
        put("Request", "Req");
        put("Response", "Resp");
        put("Authentication", "Authn");
        put("Authorization", "Authz");
        put("Encrypted", "Enc");
        put("Encryption", "Enc");
        put("Signing", "Sig");
        put("Expiration", "Exp");
        put("Object", "Obj");
        put("Token", "Tok");
    }};

    private KeyShortcuter() {
    }

    public static String shortcut(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        }

        for (String prefix : PREFIXES) {
            if (key.startsWith(prefix)) {
                key = StringUtils.removeStart(key, prefix);
            }
        }

        for (Map.Entry<String, String> replace : REPLACES.entrySet()) {
            key = StringUtils.replace(key, replace.getKey(), replace.getValue());
        }

        key = lowercaseFirstChar(key);
        return key;
    }

    public static String lowercaseFirstChar(String key) {
        return Character.toLowerCase(key.charAt(0)) + key.substring(1);
    }
}
