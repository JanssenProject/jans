/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.dev;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;
import org.xdi.model.ProgrammingLanguage;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.uma.persistence.UmaPolicy;
import org.xdi.oxauth.service.InumService;
import org.xdi.oxauth.service.uma.PolicyService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */

public class ManualComponentTest extends BaseComponentTest {

    @Override
    public void beforeClass() {
    }

    @Override
    public void afterClass() {
    }

    @Test
    public void createNewPolicy() throws IOException {
        final String inum = InumService.instance().generateInum();

        final List<String> scopes = new ArrayList<String>();
        scopes.add("http://photoz.example.com/dev/scopes/view");

        final String script = IOUtils.toString(new FileInputStream("U:\\own\\project\\oxAuth\\Server\\uma\\authorization\\python\\SamplePolicy.py"));

        final UmaPolicy umaPolicy = new UmaPolicy();
        umaPolicy.setInum(inum);
        umaPolicy.setDisplayName("Sample policy");
        umaPolicy.setDescription("Sample policy");
        umaPolicy.setProgrammingLanguage(ProgrammingLanguage.PYTHON);
        umaPolicy.setScopeDns(scopes);
        umaPolicy.setPolicyScript(script);

        final PolicyService policyService = PolicyService.instance();
        policyService.persist(umaPolicy);
    }
}
