package org.gluu.persist;

import java.util.Properties;

/**
 * Factory which creates Persistence Entry Manager
 *
 * @author Yuriy Movchan Date: 02/02/2018
 */
public interface PersistenceEntryManagerFactory {

    PersistenceEntryManager createEntryManager(Properties conf);

}
