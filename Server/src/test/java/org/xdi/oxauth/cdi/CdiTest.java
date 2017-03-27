package org.xdi.oxauth.cdi;

import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.assertNotNull;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.util.ServerUtil;


/**
 * @author Yuriy Movchan
 */
public class CdiTest {

    private WeldContainer container;

    @BeforeClass
	public void initContainer() throws Exception {
        Weld weld = new Weld();
        this.container = weld.initialize();
    }

    @AfterClass
	public void destroyContainer() throws Exception {
    	this.container.shutdown();
    }

    @Test
    public void testContainer() {
    	assertNotNull(this.container, "Failed to create Weld container");
    }


    @Test
    public void testBeanLookupByClassName() {
        ClientService clientService =  ServerUtil.bean(ClientService.class);
    	assertNotNull(clientService, "Failed to get ClientService bean");
    }

    @Test
    public void testBeanLookupByName() {
        ClientService clientService =  ServerUtil.bean(ClientService.class, "clientService");
    	assertNotNull(clientService, "Failed to get ClientService bean by name");
    }
}
