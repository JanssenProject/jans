package io.jans.as.server.service.external.context;

import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.model.token.TokenEntity;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class ExternalIntrospectionContextTest {

    @Test
    public void getTokenAttribute_whenAttributeIsPersisted_shouldReturnValue() {
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.getAttributes().getAttributes().put("ujwt", "value");
        AuthorizationGrant grant = mock(AuthorizationGrant.class);
        when(grant.getTokenEntity()).thenReturn(tokenEntity);

        ExternalIntrospectionContext context = newContext();
        context.setGrantOfIntrospectionToken(grant);

        assertEquals(context.getTokenAttribute("ujwt"), "value");
        assertNull(context.getTokenAttribute("missing"));
    }

    @Test
    public void getTokenAttribute_whenGrantIsNull_shouldReturnNull() {
        assertNull(newContext().getTokenAttribute("ujwt"));
    }

    @Test
    public void getTokenAttribute_whenTokenEntityIsNull_shouldReturnNull() {
        ExternalIntrospectionContext context = newContext();
        context.setGrantOfIntrospectionToken(mock(AuthorizationGrant.class));

        assertNull(context.getTokenAttribute("ujwt"));
    }

    private static ExternalIntrospectionContext newContext() {
        return new ExternalIntrospectionContext(null, null, null, null, null);
    }
}
