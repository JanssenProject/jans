/**
 * 
 */
package io.jans.as.server;

import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

import io.jans.util.security.SecurityProviderUtility;
import io.jans.as.model.crypto.AuthCryptoProvider;

/**
 * @author smans
 *
 */
public class BcFipsDemoTest extends BaseTest {
	
    @Test(enabled = true)
    public void bcFipsDemoTest1() {
        showTitle("BcFipsDemoTest.bcFipsDemoTest1");

        SecurityProviderUtility.installBCProvider(true);

        assertNotNull(SecurityProviderUtility.getBCProvider());
        assertNotNull(SecurityProviderUtility.getSecurityMode());

        output("------------------------------------");
        
        output("SecurityProviderUtility.getBCProvider().getName() = " + SecurityProviderUtility.getBCProvider().getName());
        output("SecurityProviderUtility.getSecurityMode()         = " + SecurityProviderUtility.getSecurityMode());

        try {
            output("------------------------------------");
        	String className = "org.bouncycastle.jcajce.interfaces.EdDSAKey"; 
			output("Loading of class: " + className);
			Class<?> classObj = this.getClass().getClassLoader().loadClass(className);
			output("classObj.getName() = " + classObj.getName());
		} catch (ClassNotFoundException ex) {
			fails(ex);
		}

        try {
        	output("------------------------------------");
        	String className = "org.bouncycastle.jcajce.spec.EdDSAParameterSpec";
			output("Loading of class: " + className);
			Class<?> classObj = this.getClass().getClassLoader().loadClass(className);
			output("classObj.getName() = " + classObj.getName());
		} catch (ClassNotFoundException ex) {
			fails(ex);
		}

        try {
        	output("------------------------------------");
        	String className = "org.bouncycastle.jcajce.interfaces.EdDSAPublicKey";
			output("Loading of class: " + className);
			Class<?> classObj = this.getClass().getClassLoader().loadClass(className);
			output("classObj.getName() = " + classObj.getName());
		} catch (ClassNotFoundException ex) {
			output("ex = " + ex.toString());
		}
        
        try {
        	output("------------------------------------");
        	String className = "org.bouncycastle.crypto.asymmetric.AsymmetricEdDSAPublicKey";
			output("Loading of class: " + className);
			Class<?> classObj = this.getClass().getClassLoader().loadClass(className);
			output("classObj.getName() = " + classObj.getName());
		} catch (ClassNotFoundException ex) {
			fails(ex);
		}
        
    	output("------------------------------------");        

        //import org.bouncycastle.jcajce.interfaces.EdDSAKey;
        //import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;        
    }

  	@Parameters ({"serverKeyStoreFile", "serverKeyStoreSecret", "dnName"})
    @Test(enabled = true)
    public void bcFipsDemoTest2(final String serverKeyStoreFile, final String serverKeyStoreSecret, final String dnName) {
        showTitle("BcFipsDemoTest.bcFipsDemoTest2");

		output("serverKeyStoreFile = " + serverKeyStoreFile);
		output("serverKeyStoreSecret = " + serverKeyStoreSecret);
		output("dnName = " + dnName);
		
		try {
			AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(serverKeyStoreFile, serverKeyStoreSecret, dnName);
			authCryptoProvider.load();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
			fails(ex);			
		}  
    }
}
