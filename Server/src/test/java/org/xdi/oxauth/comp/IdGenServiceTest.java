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
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.idgen.ws.rs.IdGenService;
import org.xdi.oxauth.model.config.Conf;
import org.xdi.oxauth.model.config.ConfigurationFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/06/2013
 */

public class IdGenServiceTest extends BaseComponentTest {

    @Override
    public void beforeClass() {
        final InputStream inputStream = InumGeneratorTest.class.getResourceAsStream("/id/gen/SampleIdGenerator.py");
        try {
            final String dn = ConfigurationFactory.getLdapConfiguration().getString("configurationEntryDN");
            final Conf conf = getLdapManager().find(Conf.class, dn);
            conf.setIdGeneratorScript(IOUtils.toString(inputStream));
            getLdapManager().merge(conf);

            ConfigurationFactory.updateFromLdap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public void afterClass() {
        try {
            final String dn = ConfigurationFactory.getLdapConfiguration().getString("configurationEntryDN");
            final Conf conf = getLdapManager().find(Conf.class, dn);
            conf.setIdGeneratorScript("");
            getLdapManager().merge(conf);
        } finally {
            ConfigurationFactory.updateFromLdap();
        }
    }

    @Test
    public void testCustomIdGenerationByPythonScript() {
        final IdGenService instance = IdGenService.instance();
        final String uuid = instance.generateId("", "");
        Assert.assertFalse(StringUtils.isNotBlank(uuid));
    }

}
