/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.uma.MetaDataConfigurationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2014
 */

public class UmaMultithreadTest {

	private String serverUri;

	private MetaDataConfigurationService service;

    @Parameters({"serverUri"})
    public UmaMultithreadTest(String serverUri) {
        this.serverUri = serverUri;
    }

    @BeforeClass
    public void before() {
        ClientConnectionManager connectoinManager = new PoolingClientConnectionManager();
        final DefaultHttpClient defaultHttpClient = new DefaultHttpClient(connectoinManager);
        final ApacheHttpClient4Executor clientExecutor = new ApacheHttpClient4Executor(defaultHttpClient);

        String url = serverUri + "/oxauth/seam/resource/restv1/oxauth/uma-configuration";

        service = UmaClientFactory.instance().createMetaDataConfigurationService(url, clientExecutor);
    }


    @Test(invocationCount = 30, threadPoolSize = 3)
    public void test() {
        final UmaConfiguration metadataConfiguration = service.getMetadataConfiguration();

        Assert.assertNotNull(metadataConfiguration);
        System.out.println(metadataConfiguration);

    }
}

