package org.gluu.oxd.rs.protect.resteasy;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/04/2016
 */

public class KeyTest {

    @Test
    public void name() {
        Key key = new Key("/photo", Lists.newArrayList("GET", "POST"));

        assertEquals("[GET, POST] /photo", key.getResourceName());
    }
}
