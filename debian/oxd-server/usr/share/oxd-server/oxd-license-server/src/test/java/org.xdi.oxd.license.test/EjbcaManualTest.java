package org.xdi.oxd.license.test;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.protocol.ws.client.gen.EjbcaWS;
import org.ejbca.core.protocol.ws.client.gen.EjbcaWSService;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.client.gen.UserMatch;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/12/2014
 */

public class EjbcaManualTest {

    private static final String CERT_PATH = "U:\\own\\project\\git\\oxd\\master\\oxd-license-server\\src\\main\\cert\\";

    @Test
    public void test() {
        CryptoProviderTools.installBCProvider();
        String urlstr = "https://ejbca.gluu.org:8443/ejbca/ejbcaws/ejbcaws?wsdl";

//        System.setProperty("javax.net.debug", "ssl:handshake");
        System.setProperty("javax.net.ssl.trustStore", CERT_PATH + "LicenseServer_TrustStore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "secret");

        System.setProperty("javax.net.ssl.keyStore", CERT_PATH +  "LicenseServer_KeyStore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "secret");

        QName qname = new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSService");
        EjbcaWSService service;
        try {
            service = new EjbcaWSService(new URL(urlstr), qname);
            EjbcaWS ejbcaraws = service.getEjbcaWSPort();

            UserMatch usermatch = new UserMatch();
            usermatch.setMatchwith(UserMatch.MATCH_WITH_DN);
            usermatch.setMatchtype(UserMatch.MATCH_TYPE_CONTAINS);
            usermatch.setMatchvalue("License");
            List<UserDataVOWS> result = ejbcaraws.findUser(usermatch);

            System.out.println(result + " : result");
            if (result.size() > 0) {
                for (UserDataVOWS userDataVOWS : result) {
                    System.out.println(userDataVOWS.getEmail());
                }
            }

        } catch (Exception ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    @Test
    public void testSoap() throws Exception {
        String url = "https://ejbca.gluu.org:8443/ejbca/ejbcaws/ejbcaws?wsdl";
        String truststore = "c:\\Temp\\ejbca\\Test\\Test\\cert\\LicenseServer_TrustStore.jks";
        String keystore = "c:\\Temp\\ejbca\\Test\\Test\\cert\\LicenseServer_KeyStore.jks";


        System.setProperty("javax.net.debug", "ssl:handshake");
        System.setProperty("javax.net.ssl.trustStore", truststore);
        System.setProperty("javax.net.ssl.trustStorePassword", "secret");

        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", "secret");

        final String keystorepassword = "secret";

        final InputStream inputStream = EjbcaManualTest.class.getResourceAsStream("createUser.xml");
        final String xml = IOUtils.toString(inputStream);
//        System.out.println(xml);
//
//        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//        FileInputStream instream = new FileInputStream(keystore);
//        try {
//            keyStore.load(instream, keystorepassword.toCharArray());
//        } finally {
//            instream.close();
//        }


//        SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore);
//        socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        HttpClient httpClient = new DefaultHttpClient();
//        httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", socketFactory, 8443));

        ClientExecutor executor = new ApacheHttpClient4Executor(httpClient);
        ClientRequest request = new ClientRequest(url, executor);
        request.body("application/xml", xml);

        // we're expecting a String back
        ClientResponse<String> response = request.post(String.class);

        if (response.getStatus() == 200) {
            String str = response.getEntity();
            System.out.println("Response: " + str);
        } else {
            System.out.println("Failed to create user");
        }

    }
}
