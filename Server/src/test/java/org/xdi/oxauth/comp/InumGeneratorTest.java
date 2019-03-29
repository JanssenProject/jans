/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.idgen.ws.rs.InumGenerator;
import org.gluu.oxauth.model.common.IdType;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public class InumGeneratorTest extends BaseComponentTest {

	@Inject
	private InumGenerator inumGenerator;

	@Test
	public void test() {
		final String inum = inumGenerator.generateId(IdType.CLIENTS, "@!1111");
		Assert.assertTrue(StringUtils.isNotBlank(inum));
	}

}
