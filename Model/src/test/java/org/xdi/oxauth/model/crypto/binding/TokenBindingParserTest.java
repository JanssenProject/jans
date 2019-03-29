package org.xdi.oxauth.model.crypto.binding;

import org.gluu.oxauth.model.crypto.binding.TokenBinding;
import org.gluu.oxauth.model.crypto.binding.TokenBindingMessage;
import org.gluu.oxauth.model.crypto.binding.TokenBindingParseException;
import org.gluu.oxauth.model.crypto.binding.TokenBindingType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingParserTest {

    @Test
    public void testParsingAndSHA256hashOfTokenBindingId() throws TokenBindingParseException {
        // values taken from spec: http://openid.net/specs/openid-connect-token-bound-authentication-1_0-03.html
        String encoded = "ARIAAgBBQCfsI1D1sTq5mvT_2H_dihNIvuHJCHGjHPJchPavNbGrOo26-2JgT_IsbvZd4daDFbirYBIwJ-TK1rh8FzrC-psAQO4Au9xPupLSkhwT9Y" +
                "n9aSvHXFsMLh4d4cEBKGP1clJtsfUFGDw-8HQSKwgKFN3WfZGq27y8NB3NAM1oNzvqVOIAAAECAEFArPIiuZxj9gK0dWhIcG63r2-sZ8V3LX9gpNl8Um_oGOtmwoP1v0VHNI" +
                "HEOzW3BOqcBLvUzVEG6a6KGEj3GrFcqQBA9YxqHPBIuDui_aQ1SoRGKyBEhaG2i-Wke3erRb1YwC7nTgrpqqJG3z1P8bt7cjZN6TpOyktdSSK7OJgiApwG7AAA";
        String expectedIdHash = "suMuxh_IlrP-Zrj33LuQOQ5rX039cmBe-wt2df3BrUQ";

        TokenBindingMessage message = new TokenBindingMessage(encoded);
        TokenBinding referredBinding = message.getFirstTokenBindingByType(TokenBindingType.REFERRED_TOKEN_BINDING);

        Assert.assertEquals(expectedIdHash, referredBinding.getTokenBindingID().sha256base64url());
    }
}
