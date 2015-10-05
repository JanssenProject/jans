package org.xdi.oxd;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.Test;
import org.xdi.oxd.server.Utils;
import org.xdi.util.security.StringEncrypter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2015
 */

public class UtilsTest {

    @Test
    public void joinAndEncode() throws UnsupportedEncodingException {
        final ArrayList<String> list = Lists.newArrayList("id_token", "token");
        Assert.assertEquals("id_token%20token", Utils.joinAndUrlEncode(list));
    }

    @Test(enabled = false)
    public void decrypt() throws StringEncrypter.EncryptionException {
        StringEncrypter stringEncrypter = StringEncrypter.instance("123456789012345678901234");
        System.out.println(stringEncrypter.decrypt(""));
    }
}
