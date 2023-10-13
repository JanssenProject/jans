package io.jans.casa.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.xel.VariableResolver;

import jakarta.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author jgomer
 */
public class CssRulesResolver implements VariableResolver {

    private static final String VARIABLE_NAME = "css";
    private static final String RULES_LOCATION = "/WEB-INF/classes/css-component-rules.properties";

    private static Logger logger = LoggerFactory.getLogger(CssRulesResolver.class);
    private static Map<String, String> rules;

    public static void init(ServletContext context) {
        try {
            Properties p = new Properties();
            p.load(context.getResourceAsStream(RULES_LOCATION));

            rules = new HashMap<>();
            p.stringPropertyNames().forEach(key -> rules.put(key, p.getProperty(key)));
            logger.info("CssRules ZK VariableResolver initialized successfully");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Object resolveVariable(String name) {
        return rules != null && VARIABLE_NAME.equals(name) ? rules : null;
    }

}
