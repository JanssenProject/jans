/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.operation;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.querydsl.core.types.OrderSpecifier;

import io.jans.orm.exception.operation.DeleteException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.EntryConvertationException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.orm.sql.impl.SqlBatchOperationWraper;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.model.SearchReturnDataType;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;

/**
 * SQL operation service interface
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public interface SqlOperationService extends PersistenceOperationService {

	String JSON_TYPE_NAME = "json";
	String JSONB_TYPE_NAME = "jsonb";
	String LONGTEXT_TYPE_NAME = "longtext";
	String TIMESTAMP = "timestamp";

    static String DN = "dn";
    static String UID = "uid";
    static String[] UID_ARRAY = new String[] { "uid" };
    static String USER_PASSWORD = "userPassword";
    static String OBJECT_CLASS = "objectClass";

    static String DOC_ALIAS = "doc";
    static String DOC_INNER_ALIAS = "doc_inner";
    static String ID = "id";
    static String DOC_ID = "doc_id";

	public static final String SQL_DATA_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	public static final Object[] NO_OBJECTS = new Object[0];

    SqlConnectionProvider getConnectionProvider();

    boolean addEntry(String key, String objectClass, Collection<AttributeData> attributes) throws DuplicateEntryException, PersistenceException;

    boolean updateEntry(String key, String objectClass, List<AttributeDataModification> mods) throws UnsupportedOperationException, PersistenceException;

    boolean delete(String key, String objectClass) throws EntryNotFoundException;
	long delete(String key, String objectClass, ConvertedExpression expression, int count) throws DeleteException;

	boolean deleteRecursively(String key, String objectClass) throws EntryNotFoundException, SearchException;

	List<AttributeData> lookup(String key, String objectClass, String... attributes) throws SearchException, EntryConvertationException;

    <O> PagedResult<EntryData> search(String key, String objectClass, ConvertedExpression expression, SearchScope scope,
            String[] attributes, OrderSpecifier<?>[] orderBy, SqlBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType,
            int start, int count, int pageSize) throws SearchException;

    String[] createStoragePassword(String[] passwords);
    
    boolean isBinaryAttribute(String attribute);
    boolean isCertificateAttribute(String attribute);

    String escapeValue(String value);
	void escapeValues(Object[] realValues);

	String unescapeValue(String value);
	void unescapeValues(Object[] realValues);

	String toInternalAttribute(String attributeName);
	String[] toInternalAttributes(String[] attributeNames);

	String fromInternalAttribute(String internalAttributeName);
	String[] fromInternalAttributes(String[] internalAttributeNames);

    boolean destroy();

	Connection getConnection();

	DatabaseMetaData getMetadata();

	boolean isJsonColumn(String tableName, String attributeType);

	TableMapping getTabeMapping(String key, String objectClass);

	String encodeTime(Date date);
    Date decodeTime(String date, boolean silent);

}
