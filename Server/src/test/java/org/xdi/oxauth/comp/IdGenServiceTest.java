/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.model.ProgrammingLanguage;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.oxauth.idgen.ws.rs.IdGenService;
import org.gluu.oxauth.model.config.ConfigurationFactory;
import org.gluu.oxauth.service.custom.CustomScriptService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.INumGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/06/2013
 */

public class IdGenServiceTest extends BaseComponentTest {

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private CustomScriptService customScriptService;

	@Inject
	private IdGenService idGenService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	private static String idCustomScriptDn;

	private CustomScript buildIdCustomScriptEntry(String idScript) {
		String customScriptId = UUID.randomUUID().toString();
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

	@Test
	public void loadCustomScript() {
		final InputStream inputStream = IdGenServiceTest.class.getResourceAsStream("/id/gen/SampleIdGenerator.py");
		try {
			final String idScript = IOUtils.toString(inputStream);
			CustomScript idCustomScript = buildIdCustomScriptEntry(idScript);
			this.idCustomScriptDn = idCustomScript.getDn();

			ldapEntryManager.persist(idCustomScript);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	@Test(dependsOnMethods = "loadCustomScript")
	public void testCustomIdGenerationByPythonScript() {
		final String uuid = idGenService.generateId("test", "");
		System.out.println("Generated Id: " + uuid);
		Assert.assertFalse(StringUtils.isNotBlank(uuid));

		final String invalidUuid = idGenService.generateId("", "");
		System.out.println("Generated invalid Id: " + invalidUuid);
		Assert.assertFalse(StringUtils.equalsIgnoreCase(invalidUuid, "invalid"));
	}

	@Test(dependsOnMethods = "testCustomIdGenerationByPythonScript")
	public void removeCustomScript() {
		CustomScript customScript = new CustomScript();
		customScript.setDn(this.idCustomScriptDn);

		ldapEntryManager.remove(customScript);
	}

}
