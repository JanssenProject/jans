/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogEscaperTest {

	@Test
	void testEscapeLog_Null_ReturnsEmpty() {
		assertEquals("", LogEscaper.escapeLog(null));
	}

	@Test
	void testEscapeLog_LineBreaksAndTabs_Replaced() {
		assertEquals("a_b_c_d", LogEscaper.escapeLog("a\nb\rc\td"));
	}

	@Test
	void testEscapeLog_EscapeSequenceAndDel_Replaced() {
		assertEquals("_[31mred_", LogEscaper.escapeLog("\u001b[31mred\u007f"));
	}

	@Test
	void testEscapeLog_PlainText_Unchanged() {
		assertEquals("record-1 ok", LogEscaper.escapeLog("record-1 ok"));
	}

}
