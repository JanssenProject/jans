package org.gluu.site.ldap;

import com.unboundid.ldap.sdk.LDAPException;
import org.gluu.site.ldap.persistence.LdapEntryManager;

import java.util.Properties;

public class TestConnectionProvider {

    public static void main(String[] args) throws InterruptedException, LDAPException {
        Properties ldapProperties = new Properties();
        ldapProperties.put("bindDN", "cn=directory manager");
        ldapProperties.put("bindPassword", "secret");
//        ldapProperties.put("servers", "localhost:1389, localhost:2389");
        ldapProperties.put("maxconnections", "3");
        ldapProperties.put("useSSL", "true");

        LDAPConnectionProvider connectionProvider = new LDAPConnectionProvider(ldapProperties);
        OperationsFacade operationsFacade = new OperationsFacade(connectionProvider);

//        Filter filterUid = Filter.createEqualityFilter("uid", "yuriy");

        try {
            for (int i = 0; i < 10000; i++) {
                LdapEntryManager m = new LdapEntryManager(operationsFacade);

                System.out.println(operationsFacade.getConnection());

//                LDAPConnection connection = operationsFacade.getConnection();
//                try {
//
//                } finally {
//                    operationsFacade.releaseConnection(connection);
//                }
                final DummyEntry dummyEntry = m.find(DummyEntry.class, "o=gluu");
//                SearchResult result = operationsFacade.search("ou=people,o=@!1111,o=gluu", filterUid, 1);
                System.out.println(dummyEntry);

                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
