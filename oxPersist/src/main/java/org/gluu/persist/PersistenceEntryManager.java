package org.gluu.persist;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ListViewResponse;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.search.filter.Filter;

/**
 * Methods which Entry Manager must provide
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface PersistenceEntryManager {

    void persist(Object entry);

    <T> T merge(T entry);

    void remove(Object entry);

    void removeRecursively(String dn);

    boolean contains(Object entry);

    <T> boolean contains(Class<T> entryClass, String primaryKey);

    <T> boolean contains(String primaryKey, Class<T> entryClass, Filter filter);

    <T> List<T> findEntries(Object entry);

    <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes);

    /**
     * Search by sample
     *
     * @param entry
     *            Sample
     * @param sizeLimit
     *            Maximum result set size
     * @return Result entries
     */
    <T> List<T> findEntries(Object entry, int sizeLimit);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, int sizeLimit);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, int sizeLimit);

    /**
     * Search from baseDN
     *
     * @param baseDN
     * @param entryClass
     * @param filter
     * @param scope
     * @param ldapReturnAttributes
     * @param sizeLimit
     * @param chunkSize
     *            Specify LDAP/DB pagination data set size
     * @return Result entries
     */
    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes, int sizeLimit,
            int chunkSize);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int startIndex, int sizeLimit, int chunkSize);

    <T> ListViewResponse<T> findListViewResponse(String baseDN, Class<T> entryClass, Filter filter, int startIndex, int count, int chunkSize,
            String sortBy, SortOrder sortOrder, String[] ldapReturnAttributes);

    boolean authenticate(String bindDn, String password);

    boolean authenticate(String userName, String password, String baseDN);

    <T> int countEntries(Object entry);

    <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter);

    int getHashCode(Object entry);

    String[] getObjectClasses(Object entry, Class<?> entryClass);

    <T> List<T> createEntities(Class<T> entryClass, Map<String, List<AttributeData>> entriesAttributes);

    String encodeGeneralizedTime(Date date);

    Date decodeGeneralizedTime(String date);

    // TODO: use close
    boolean destroy();

    <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String... sortByProperties);

    <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String groupByProperties,
            String sumByProperties);

    void addDeleteSubscriber(DeleteNotifier subscriber);

    void removeDeleteSubscriber(DeleteNotifier subscriber);

}
