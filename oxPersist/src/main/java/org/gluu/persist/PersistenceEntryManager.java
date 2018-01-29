package org.gluu.persist;

import java.util.Date;
import java.util.List;

import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.SearchScope;
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
	<T> boolean contains(String baseDN, Class<T> entryClass, Filter filter);

	<T> List<T> findEntries(Object entry);

	/**
	 * Search by sample
	 * 
	 * @param entry Sample
	 * @param sizeLimit Maximum result set size  
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
	 * @param chunkSize Specify LDAP/DB pagination data set size  
	 * @return Result entries
	 */
	<T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes, int sizeLimit, int chunkSize);
	<T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes, BatchOperation<T> batchOperation, int startIndex, int sizeLimit, int chunkSize);

	boolean authenticate(String bindDn, String password);
	boolean authenticate(String userName, String password, String baseDN);

	<T> int countEntries(Object entry);
	<T> int countEntries(String baseDN, Class<T> entryClass, Filter filter);

	int getHashCode(Object entry);

	public String encodeGeneralizedTime(Date date);
	Date decodeGeneralizedTime(String date);

	boolean destroy();

	void addDeleteSubscriber(DeleteNotifier subscriber);
	void removerDeleteSubscriber(DeleteNotifier subscriber);

	// TODO: 3.2.0: Change name
	String[] getLDIF(String dn);

	// TODO: 3.2.0: Change name
	List<String[]> getLDIF(String dn, String[] attributes);

	// TODO: 3.2.0: Change name
	List<String[]> getLDIFTree(String baseDN, Filter searchFilter, String... attributes);

	// TODO: 3.2.0: Change name
	int getSupportedLDAPVersion();

}