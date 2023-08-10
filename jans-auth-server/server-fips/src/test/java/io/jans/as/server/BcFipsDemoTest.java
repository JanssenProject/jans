/**
 * 
 */
package io.jans.as.server;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import io.jans.util.security.SecurityProviderUtility;

/**
 * @author smans
 *
 */
public class BcFipsDemoTest extends BaseTest {
	
    @Test(enabled = true)
    public void bcFipsDemoTest() {
        showTitle("BcFipsDemoTest.bcFipsDemoTest");

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
}
