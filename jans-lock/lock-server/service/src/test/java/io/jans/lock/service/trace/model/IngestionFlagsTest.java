/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IngestionFlagsTest {

	@Test
	void reservedMembers_cappedAndCopied() {
		List<String> members = new ArrayList<>();
		for (int i = 0; i < IngestionFlags.MAX_RESERVED_MEMBERS + 5; i++) {
			members.add("trace.event.decisions[" + i + "].late_flag");
		}
		IngestionFlags flags = new IngestionFlags(false, false, false, false);

		flags.setReservedMembers(members);
		members.clear();

		assertTrue(flags.isReservedMember());
		assertEquals(IngestionFlags.MAX_RESERVED_MEMBERS, flags.getReservedMembers().size());
		assertEquals(flags, flags.copy());
	}

	@Test
	void reservedMembers_nullMeansNone() {
		IngestionFlags flags = new IngestionFlags(false, false, false, false);

		flags.setReservedMembers(null);

		assertFalse(flags.isReservedMember());
		assertTrue(flags.getReservedMembers().isEmpty());
	}

}
