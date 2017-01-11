/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.idgen.ws.rs.InumGenerator;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public class InumGeneratorTest extends BaseComponentTest {

    @Test
    public void test() {
        final InumGenerator inumGenerator = ServerUtil.instance(InumGenerator.class);
        final String inum = inumGenerator.generateId(IdType.CLIENTS, "@!1111");
        Assert.assertTrue(StringUtils.isNotBlank(inum));

//        final boolean contains = inumGenerator.contains("@!1111!0008!298D.5B20", IdType.LINK_CONTRACTS);
//        Assert.assertTrue(contains);
    }

    @Override
    public void beforeClass() {
    }

    @Override
    public void afterClass() {
    }
}
