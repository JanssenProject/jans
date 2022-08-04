/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.model.common.IdType;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.idgen.ws.rs.InumGenerator;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public class InumGeneratorTest extends BaseComponentTest {

    @Test
    public void test() {
        InumGenerator inumGenerator = getInumGenerator();
        final String inum = inumGenerator.generateId(IdType.CLIENTS, "@!1111");
        Assert.assertTrue(StringUtils.isNotBlank(inum));
    }

}
