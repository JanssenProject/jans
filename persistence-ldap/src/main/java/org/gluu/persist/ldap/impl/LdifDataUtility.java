/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.ldap.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.gluu.util.StringHelper;

import com.unboundid.ldap.sdk.ChangeType;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;

/**
 * Utility class to import ldif file to LDAP.
 *
 * @author Yuriy Movchan Date: 08.06.2010
 */
public final class LdifDataUtility {

    private static final Logger LOG = Logger.getLogger(LdifDataUtility.class);

    // Just define the singleton as a static field in a separate class.
    // The semantics of Java guarantee that the field will not be initialized until
    // the field is referenced,
    // and that any thread which accesses the field will see all of the writes
    // resulting from initializing that field.
    // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
    private static class Holder {
        private static LdifDataUtility INSTANCE = new LdifDataUtility();
    }

    private LdifDataUtility() {
    }

    public static LdifDataUtility instance() {
        return Holder.INSTANCE;
    }

    /**
     * Performs ldif file import
     *
     * @param connection
     *            Connection to LDAP server
     * @param ldifFileName
     *            LDIF file
     * @return The result code for the processing that was performed
     */
    public ResultCode importLdifFile(LDAPConnection connection, String ldifFileName) {
        LDIFReader ldifReader = createLdifReader(ldifFileName);
        if (ldifReader == null) {
            return ResultCode.LOCAL_ERROR;
        }
        try {
            return importLdifFile(connection, ldifReader);
        } finally {
            disposeLdifReader(ldifReader);
        }
    }

    /**
     * Performs ldif file conent import
     *
     * @param connection
     *            Connection to LDAP server
     * @param ldifFileContent
     *            LDIF file
     * @return The result code for the processing that was performed
     */
    public ResultCode importLdifFileContent(LDAPConnection connection, String ldifFileContent) {
        BufferedReader is = null;
        LDIFReader ldifReader = null;
        try {
            is = new BufferedReader(new StringReader(ldifFileContent));
            ldifReader = new LDIFReader(is);

            return importLdifFile(connection, ldifReader);
        } finally {
            IOUtils.closeQuietly(is);
            if (ldifReader != null) {
                disposeLdifReader(ldifReader);
            }
        }
    }

    /**
     * Performs ldif file import
     *
     * @param connection
     *            Connection to LDAP server
     * @param ldifReader
     *            LDIF reader
     * @return The result code for the processing that was performed
     */
    public ResultCode importLdifFile(LDAPConnection connection, LDIFReader ldifReader) {
        // Attempt to process and apply the changes to the server
        ResultCode resultCode = ResultCode.SUCCESS;
        while (true) {
            // Read the next change to process
            LDIFChangeRecord ldifRecord = null;
            try {
                ldifRecord = ldifReader.readChangeRecord(true);
            } catch (LDIFException le) {
                LOG.error("Malformed ldif record", le);
                if (!le.mayContinueReading()) {
                    resultCode = ResultCode.DECODING_ERROR;
                    break;
                }
            } catch (IOException ioe) {
                LOG.error("I/O error encountered while reading a change record", ioe);
                resultCode = ResultCode.LOCAL_ERROR;
                break;
            }

            // If the change record was null, then it means there are no more
            // changes to be processed.
            if (ldifRecord == null) {
                break;
            }

            // Apply the target change to the server.
            try {
                ldifRecord.processChange(connection);
            } catch (LDAPException le) {
                if (ResultCode.ENTRY_ALREADY_EXISTS.equals(le.getResultCode())) {
                    continue;
                }
                if (ldifRecord.getChangeType().equals(ChangeType.DELETE)) {
                    continue;
                }

                LOG.error("Failed to inserting ldif record", le);
            }
        }

        return resultCode;
    }

    /**
     * Check if DS has at least one DN simular to specified in ldif file.
     *
     * @param connection
     *            Connection to LDAP server
     * @param ldifFileName
     *            LDIF file
     * @return true if server contains at least one DN simular to specified in ldif
     *         file.
     */
    public boolean checkIfSerrverHasEntryFromLDIFFile(LDAPConnection connection, String ldifFileName) {
        // Set up the LDIF reader that will be used to read the changes to apply
        LDIFReader ldifReader = createLdifReader(ldifFileName);
        if (ldifReader == null) {
            return true;
        }

        // Check all ldif entries
        while (true) {
            // Read the next change to process.
            Entry entry = null;
            try {
                entry = ldifReader.readEntry();
            } catch (LDIFException le) {
                LOG.error("Malformed ldif record", le);
                if (!le.mayContinueReading()) {
                    return true;
                }
            } catch (IOException ioe) {
                LOG.error("I/O error encountered while reading a change record", ioe);
                return true;
            }

            // If the change record was null, then it means there are no more
            // changes to be processed.
            if (entry == null) {
                break;
            }

            // Search entry in the server.
            try {
                SearchResult sr = connection.search(entry.getDN(), SearchScope.BASE, "objectClass=*");
                if ((sr != null) && (sr.getEntryCount() > 0)) {
                    return true;
                }
            } catch (LDAPException le) {
                if (le.getResultCode() != ResultCode.NO_SUCH_OBJECT) {
                    LOG.error("Failed to search ldif record", le);
                    return true;
                }
            }
        }

        disposeLdifReader(ldifReader);

        return false;
    }

    /**
     * Remove base entry with all sub entries
     *
     * @param connection
     *            Connection to LDAP server
     * @param baseDN
     *            Base DN entry
     * @return The result code for the processing that was performed.
     */
    public ResultCode deleteEntryWithAllSubs(LDAPConnection connection, String baseDN) {
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = null;
        try {
            searchResult = connection.search(baseDN, SearchScope.SUB, "objectClass=*");
            if ((searchResult == null) || (searchResult.getEntryCount() == 0)) {
                return ResultCode.LOCAL_ERROR;
            }
        } catch (LDAPSearchException le) {
            LOG.error("Failed to search subordinate entries", le);
            return ResultCode.LOCAL_ERROR;
        }

        LinkedList<String> dns = new LinkedList<String>();
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            dns.add(entry.getDN());
        }

        ListIterator<String> listIterator = dns.listIterator(dns.size());
        while (listIterator.hasPrevious()) {
            try {
                connection.delete(listIterator.previous());
            } catch (LDAPException le) {
                LOG.error("Failed to delete entry", le);
                resultCode = ResultCode.LOCAL_ERROR;
                break;
            }
        }

        return resultCode;
    }

    private LDIFReader createLdifReader(String ldifFileNamePath) {
        // Set up the LDIF reader that will be used to read the changes to apply
        File ldifFile = new File(ldifFileNamePath);
        LDIFReader ldifReader;
        try {
            if (!ldifFile.exists()) {
                return null;
            }
            ldifReader = new LDIFReader(ldifFile);
        } catch (IOException ex) {
            LOG.error("I/O error creating the LDIF reader", ex);
            return null;
        }

        return ldifReader;
    }

    private void disposeLdifReader(LDIFReader ldifReader) {
        if (ldifReader != null) {
            try {
                ldifReader.close();
            } catch (IOException ex) {
            }
        }
    }

    public ResultCode validateLDIF(LDIFReader ldifReader, String dn) {
        String baseDn = dn.toLowerCase();
        ResultCode resultCode = ResultCode.SUCCESS;
        while (true) {
            // Read the next change to process
            LDIFChangeRecord ldifRecord = null;
            try {
                ldifRecord = ldifReader.readChangeRecord(true);
                if (ldifRecord != null) {
                    if (StringHelper.isNotEmpty(baseDn)) {
                        if (!ldifRecord.getDN().toLowerCase().endsWith(baseDn)) {
                            resultCode = ResultCode.NOT_SUPPORTED;
                            break;
                        }
                    }
                }

            } catch (LDIFException le) {
                LOG.info("Malformed ldif record " + ldifRecord);
                LOG.error("Malformed ldif record", le);
                resultCode = ResultCode.DECODING_ERROR;
                break;
            } catch (IOException ioe) {
                LOG.error("I/O error encountered while reading a change record", ioe);
                resultCode = ResultCode.LOCAL_ERROR;
                break;
            }

            // If the change record was null, then it means there are no more
            // changes to be processed.
            if (ldifRecord == null) {
                break;
            }
        }

        return resultCode;
    }

    public List<SearchResultEntry> getAttributeResultEntryLDIF(LDAPConnection connection, List<String> patterns, String baseDN) {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        try {
            for (String pattern : patterns) {
                String[] targetArray = new String[] {pattern};
                Filter inumFilter = Filter.createSubstringFilter("inum", null, targetArray, null);
                Filter searchFilter = Filter.createORFilter(inumFilter);
                SearchResultEntry sr = connection.searchForEntry(baseDN, SearchScope.SUB, searchFilter, null);
                searchResultEntryList.add(sr);
            }

            return searchResultEntryList;
        } catch (LDAPException le) {
            if (le.getResultCode() != ResultCode.NO_SUCH_OBJECT) {
                LOG.error("Failed to search ldif record", le);
                return null;
            }
        }
        return null;
    }

}
