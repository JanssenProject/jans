/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.server.BaseTest;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaTestUtil;
import io.jans.as.server.model.uma.TUma;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/03/2013
 */

public class UmaConfigurationWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	@Parameters({ "umaConfigurationPath" })
	@Test
	public void configurationPresence(final String umaConfigurationPath) throws Exception {
		final UmaMetadata c = TUma.requestConfiguration(url, umaConfigurationPath);
		UmaTestUtil.assert_(c);
	}
}
