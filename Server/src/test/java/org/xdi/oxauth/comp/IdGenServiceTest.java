/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.model.ProgrammingLanguage;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.model.CustomScript;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.idgen.ws.rs.IdGenService;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.service.custom.CustomScriptService;
import org.xdi.util.INumGenerator;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/06/2013
 */

public class IdGenServiceTest extends BaseComponentTest {

	private String idCustomScriptDn;

	private CustomScript buildIdCustomScriptEntry(String idScript) {
		final CustomScriptService customScriptService = (CustomScriptService) Component.getInstance(CustomScriptService.class);

		String basedInum = ConfigurationFactory.instance().getConfiguration().getOrganizationInum();
		String customScriptId = basedInum + "!" + INumGenerator.generate(2);
		String dn = customScriptService.buildDn(customScriptId);

		CustomScript customScript = new CustomScript();
		customScript.setDn(dn);
		customScript.setInum(customScriptId);
		customScript.setProgrammingLanguage(ProgrammingLanguage.PYTHON);
		customScript.setScriptType(CustomScriptType.ID_GENERATOR);

		customScript.setScript(idScript);

		customScript.setName("test_id");
		customScript.setLevel(0);
		customScript.setEnabled(true);
		customScript.setRevision(1);

		return customScript;
	}

	@Override
	public void beforeClass() {
		final InputStream inputStream = InumGeneratorTest.class.getResourceAsStream("/id/gen/SampleIdGenerator.py");
		try {
			final String idScript = IOUtils.toString(inputStream);
			CustomScript idCustomScript = buildIdCustomScriptEntry(idScript);
			this.idCustomScriptDn = idCustomScript.getDn();

			getLdapManager().persist(idCustomScript);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	@Override
	public void afterClass() {
		CustomScript customScript = new CustomScript();
		customScript.setDn(this.idCustomScriptDn);

		getLdapManager().remove(customScript);
	}

	@Test
	public void testCustomIdGenerationByPythonScript() {
		final IdGenService instance = IdGenService.instance();

		final String uuid = instance.generateId("test", "");
		System.out.println("Generated Id: " + uuid);
		Assert.assertFalse(StringUtils.isNotBlank(uuid));

		final String invalidUuid = instance.generateId("", "");
		System.out.println("Generated invalid Id: " + invalidUuid);
		Assert.assertFalse(StringUtils.equalsIgnoreCase(invalidUuid, "invalid"));
	}

}
