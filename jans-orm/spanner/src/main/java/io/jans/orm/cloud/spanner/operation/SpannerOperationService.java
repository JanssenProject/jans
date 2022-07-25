/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Type.StructField;

import io.jans.orm.cloud.spanner.impl.SpannerBatchOperationWraper;
import io.jans.orm.cloud.spanner.model.ConvertedExpression;
import io.jans.orm.cloud.spanner.model.SearchReturnDataType;
import io.jans.orm.cloud.spanner.model.TableMapping;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
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
import io.jans.orm.model.Sort;
import io.jans.orm.operation.PersistenceOperationService;

/**
 * SQL operation service interface
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public interface SpannerOperationService extends PersistenceOperationService {

    static String DN = "dn";
    static String UID = "uid";
    static String[] UID_ARRAY = new String[] { "uid" };
    static String USER_PASSWORD = "userPassword";
    static String OBJECT_CLASS = "objectClass";

    static String DOC_ALIAS = "doc";
    static String ID = "id";
    static String DOC_ID = "doc_id";
    static String DICT_DOC_ID = "dict_doc_id";

	public static final Object[] NO_OBJECTS = new Object[0];

    SpannerConnectionProvider getConnectionProvider();

    boolean addEntry(String key, String objectClass, Collection<AttributeData> attributes) throws DuplicateEntryException, PersistenceException;

    boolean updateEntry(String key, String objectClass, List<AttributeDataModification> mods) throws UnsupportedOperationException, PersistenceException;

    boolean delete(String key, String objectClass) throws EntryNotFoundException;
	long delete(String key, String objectClass, ConvertedExpression expression, int count) throws DeleteException;

	boolean deleteRecursively(String key, String objectClass) throws EntryNotFoundException, SearchException;

	List<AttributeData> lookup(String key, String objectClass, String... attributes) throws SearchException, EntryConvertationException;

    <O> PagedResult<EntryData> search(String key, String objectClass, ConvertedExpression expression, SearchScope scope,
            String[] attributes, Sort[] orderBy, SpannerBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType,
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

	DatabaseClient getConnection();

	Map<String, Map<String, StructField>> getMetadata();

	TableMapping getTabeMapping(String key, String objectClass);

	Set<String> getTabeChildAttributes(String objectClass);

}
