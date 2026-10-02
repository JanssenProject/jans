/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.operation.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.unboundid.ldap.sdk.Modification;

import io.jans.orm.exception.UnsupportedOperationException;

/**
 * Unit test for task 05 acceptance criterion 4: the RFC 4528 assertion-control capability check
 * in {@link LdapOperationServiceImpl#updateEntryWithVersion} must reject -- before sending any
 * modify request -- when the connected server does not advertise the control. No live server:
 * {@link LdapConnectionProvider} is mocked directly, since its own capability check
 * ({@code isSupportsAssertionRequestControl}) is exactly the thing being stood in for.
 */
public class LdapOperationServiceImplUpdateWithVersionTest {

	@Test
	public void updateEntryWithVersionThrowsAndSendsNoModifyRequestWhenControlNotAdvertised() {
		LdapConnectionProvider connectionProvider = mock(LdapConnectionProvider.class);

		LdapOperationServiceImpl operationService = new LdapOperationServiceImpl(connectionProvider);

		// Construction itself does unrelated schema introspection through the connection
		// provider -- isolate verification below to only what updateEntryWithVersion does.
		clearInvocations(connectionProvider);
		when(connectionProvider.isSupportsAssertionRequestControl()).thenReturn(false);

		List<Modification> modifications = Collections.emptyList();

		UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
				() -> operationService.updateEntryWithVersion("uid=test,o=jans", modifications, "jansversion", 5L));
		assertTrue(ex.getMessage().contains("1.3.6.1.1.12"));

		// The guard must short-circuit before any modify request is built/sent to the server.
		verify(connectionProvider, never()).getConnectionPool();
	}

}
