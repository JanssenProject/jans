package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.Test;
import org.gluu.util.security.StringEncrypter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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

    @Test
    public void hoursDiff() {
        Calendar calendar = Calendar.getInstance();
        Date today = new Date();

        calendar.add(Calendar.HOUR, 13);

        Assert.assertEquals(Utils.hoursDiff(today, calendar.getTime()), 13);
    }

    public static void main(String[] args) {
        String s = "{\"command\":\"register_site\",\"params\" : {\"authorization_redirect_uri\" : \"https://opencart.gl/index.php?route=module/socl_login&logout_from_gluu=aruesa\",\"post_logout_redirect_uri\" : \"https://opencart.gl/index.php?route=module/socl_login&logout_from_gluu=exist\",\"application_type\" : \"web\",\"redirect_uris\" :[\"https://opencart.gl/index.php?route=module/socl_login\"],\"acr_values\" : [],\"scope\" : [\"openid\",\"profile\",\"email\",\"address\",\"clientinfo\",\"mobile_phone\",\"phone\"],\"client_jwks_uri\" : null,\"client_token_endpoint_auth_method\" : null,\"client_request_uris\" : null,\"contacts\" : [\"vlad.karapetyan.1988@mail.ru\"],\"grant_types\" : [\"authorization_code\"],\"response_types\" : [\"code\"],\"client_logout_uris\" : [\"https://opencart.gl/index.php?route=module/socl_login&logout_from_gluu=exist\"]}}";
        System.out.println(s.length());
    }
}
