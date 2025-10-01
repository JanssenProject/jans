package io.jans.as.server.model.common;

import io.jans.as.model.common.TokenType;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
public class TxTokenTest {

    @Test
    public void newTxToken_shouldHaveCorrectTokenType() {
        TxToken txToken = new TxToken(1);
        assertEquals(TokenType.N_A, txToken.getTokenType());
    }
}
