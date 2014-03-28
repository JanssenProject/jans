package org.gluu.oxauth.client.util;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xdi.util.StringHelper;

/**
 * Abstracts out the ability to configure the filters from the initial properties provided
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public abstract class AbstractConfigurationFilter implements Filter {

	protected final Log log = LogFactory.getLog(getClass());

    /**
     * Retrieves the property from the FilterConfig.  First it checks the FilterConfig's initParameters to see if it has a value.
     * If it does, it returns that, otherwise it retrieves the ServletContext's initParameters and returns that value if any.
     *
     * @param filterConfig the Filter Configuration.
     * @param propertyName the property to retrieve.
     * @param defaultValue the default value if the property is not found.
     * @return the property value, following the above conventions.  It will always return the more specific value (i.e. filter vs. context).
     */
    protected final String getPropertyFromInitParams(final FilterConfig filterConfig, final String propertyName, final String defaultValue)  {
        final String value = filterConfig.getInitParameter(propertyName);

        if (StringHelper.isNotEmpty(value)) {
            log.info("Property [" + propertyName + "] loaded from FilterConfig.getInitParameter with value [" + value + "]");
            return value;
        }

        final String value2 = filterConfig.getServletContext().getInitParameter(propertyName);
        if (StringHelper.isNotEmpty(value2)) {
            log.info("Property [" + propertyName + "] loaded from ServletContext.getInitParameter with value [" + value2 + "]");
            return value2;
        }

        final String value3 = Configuration.instance().getPropertyValue(propertyName);
        if (StringHelper.isNotEmpty(value3)) {
            log.info("Property [" + propertyName + "] loaded from oxTrust.properties");
            return value3;
        }

        log.info("Property [" + propertyName + "] not found.  Using default value [" + defaultValue + "]");
        return defaultValue;
    }

}
